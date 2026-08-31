import java.io.*;

public class BenchmarkGenerator {
    public static void main(String[] args) throws Exception {
        String targetPath = args.length > 0 ? args[0] : "benchmark_data.xml";
        int targetSizeMB = args.length > 1 ? Integer.parseInt(args[1]) : 10;

        File outFile = new File(targetPath);
        long targetBytes = (long) targetSizeMB * 1024 * 1024;
        int id = 1;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile), 65536)) {
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            bw.write("<catalog xmlns:ns=\"http://example.com/ns\" xmlns:meta=\"http://example.com/meta\">\n");

            long written = 100;
            while (written < targetBytes) {
                String item = "  <item id=\"" + id + "\" meta:status=\"active\" ns:type=\"product\">\n" +
                              "    <name>Product Item Number " + id + "</name>\n" +
                              "    <description>This is a realistic benchmark test item containing sample alphanumeric text with some entities like &amp; &lt; &gt; and attributes.</description>\n" +
                              "    <price currency=\"EUR\">" + (10 + (id % 500)) + ".99</price>\n" +
                              "    <details count=\"" + (id % 10) + "\" inStock=\"true\">\n" +
                              "      <spec key=\"weight\">" + (id % 50) + "kg</spec>\n" +
                              "      <spec key=\"dimension\">" + (id % 100) + "x" + (id % 100) + "cm</spec>\n" +
                              "    </details>\n" +
                              "  </item>\n";
                bw.write(item);
                written += item.length();
                id++;
            }

            bw.write("</catalog>\n");
        }
        System.out.println("Generated XML test file: " + outFile.getAbsolutePath() + " (" + String.format("%.2f", outFile.length() / (1024.0 * 1024.0)) + " MB, " + id + " records)");
    }
}
