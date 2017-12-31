package uni3D;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import utils.ArrayUtils;
import utils.ButtonMessages;
import utils.ImageUtils;
import utils.InputOutput;
import utils.AboutBox;
import utils.MyCircleDetector;
import utils.MyConst;
import utils.MyGenericDialogGrid;
import utils.MyLine;
import utils.MyLog;
import utils.MyPlot;
import utils.MyStackUtils;
import utils.MyVersionUtils;
import utils.ReadDicom;
import utils.UtilAyv;

//=====================================================
//     Elaborazioni Mask
//     13 ottobre 2017 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ

//=====================================================

public class ItalianCSV_ implements PlugIn {

	public void run(String arg) {

		new AboutBox().about("ItalianCSV", MyVersionUtils.CURRENT_VERSION);
		IJ.wait(2000);
		new AboutBox().close();

		do {
			OpenDialog dd2 = new OpenDialog("SELEZIONARE IL FILE CSV DA ITALIANIZZARE");
			String path1 = dd2.getPath();
			if (path1 == null)
				return;
			File csvFile = new File(path1);
			InputOutput io = new InputOutput();
			String aux1 = io.extractDirectory(path1);
			String aux2 = io.extractFileName(path1);
			IJ.log("aux2= " + aux2);
			int sep = aux2.lastIndexOf(".");
			String aux3 = aux2.substring(0, sep);
			IJ.log(aux3);
			File outFile = new File(aux1 + "\\" + aux3 + ".tmp");
			try {
				copyFile(csvFile, outFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			File old1 = new File(aux1 + "\\" + aux3 + ".csv");
			File new1 = new File(aux1 + "\\" + aux3 + ".bak");
			boolean success1 = old1.renameTo(new1);
			File old2 = new File(aux1 + "\\" + aux3 + ".tmp");
			File new2 = new File(aux1 + "\\" + aux3 + ".csv");
			boolean success2 = old2.renameTo(new2);
			if (success1 && success2)
				IJ.log("FINE");
			else
				IJ.log("HOUSTON ABBIAMO UN PROBLEMA");
		} while (true);
	}

	private static void copyFile(File source, File dest) throws IOException {

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
		int ch;
		while ((ch = in.read()) != -1) {
			if (ch == ',')
				ch = ';';
			out.write(ch);
		}
		out.close();
		in.close();
	}

}
