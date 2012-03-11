package net.vtst.ow.closure.compiler.strip;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;

import net.vtst.ow.closure.compiler.magic.MagicException;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.closure.compiler.util.FileTreeVisitor;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.ErrorManager;

/**
 * Run JSFileStripper on a set of JavaScript files.
 * 
 * @author Vincent Simonet
 */
public class JSLibraryStripper {
  
  private JSFileStripper fileStripper;
  private PrintStream outputStream;
  private ErrorManager errorManager;
  
  private int number_of_directories = 0;
  private int number_of_files = 0;
  
  
  public JSLibraryStripper() {
    this(System.out, System.err);
  }
  
  public JSLibraryStripper(PrintStream out, PrintStream err) {
    this.outputStream = out;
    this.errorManager = CompilerUtils.makePrintingErrorManager(err);
    this.fileStripper = new JSFileStripper(this.errorManager);
  }
  
  
  /**
   * Recursively run the stripper on all .js files containing in the input
   * directory, and output the stripped files in output following the same
   * structure.
   * @param input  The path of the input directory.
   * @param output  The path of the output directory.
   * @throws IOException
   */
  public void strip(File input, File output) throws IOException {
    FileTreeVisitor.File<IOException> visitor = new FileTreeVisitor.File<IOException>() {
      public boolean preVisitDirectory(File input, File output) {
        ++number_of_directories;
        if (!output.exists()) output.mkdir();
        return true;
      }
      public void visitFile(File input, File output) throws IOException {
        if (!CompilerUtils.isJavaScriptFile(input)) return;
        ++number_of_files;
        outputStream.println("Processing: " + input.getAbsolutePath());
        fileStripper.strip(input, output);
      }
    };
    visitor.visit(input, output);
  }

  
  /**
   * Output a summary message at the end of the process.
   */
  public void printSummary() {
    errorManager.generateReport();
    int number_of_errors = errorManager.getErrorCount();
    outputStream.print("Processed ");
    outputStream.print(number_of_files);
    outputStream.print(number_of_files > 0 ? " files in " : " file in ");
    outputStream.print(number_of_directories);
    outputStream.println(number_of_directories > 0 ? " directories." : " directory");
    outputStream.print(number_of_errors);
    outputStream.println(number_of_errors > 0 ? " errors." : " error.");
  }

  
  public static void main(String[] args) {
    if (args.length != 3) {
      System.err.println("Must be run with 2 arguments.");
    }
    Compiler.setLoggingLevel(Level.OFF);
    JSLibraryStripper stripper = new JSLibraryStripper();
    try {
      stripper.strip(new File(args[1]), new File(args[2]));
      stripper.printSummary();
    } catch (MagicException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
