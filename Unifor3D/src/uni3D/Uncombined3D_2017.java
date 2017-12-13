package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.File;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import utils.AboutBox;
import utils.ArrayUtils;
import utils.ButtonMessages;
import utils.ImageUtils;
import utils.InputOutput;
import utils.MyCircleDetector;
import utils.MyColor;
import utils.MyConst;
import utils.MyFilter;
import utils.MyFwhm;
import utils.MyGenericDialogGrid;
import utils.MyLine;
import utils.MyLog;
import utils.MyPlot;
import utils.MySphere;
import utils.MyStackUtils;
import utils.MyTimeUtils;
import utils.ReadDicom;
import utils.UtilAyv;

//=====================================================
//     Programma per plot 3D per immagini uncombined circolari
//     1 settembre 2016 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class Uncombined3D_2017 implements PlugIn {
	static boolean debug1 = false;
	final static int timeout = 100;
	public static String VERSION = "CDQ 3D";

	static boolean stampa = true;
	static boolean stampa2 = true;
	static int debugXX = 120;
	static int debugYY = 90;
	static int debugZZ = 80;
	static int puntatore = 0;

	public void run(String arg) {

		boolean step = false;
		boolean paintPixels = false;

		int diam7x7 = 16;
		int diam11x11 = 20;
		// int diam11x11 = 20;
		boolean demo0 = false;
		int debuglevel = 0;
		ResultsTable rt1 = ResultsTable.getResultsTable();
		new AboutBox().about("Uncombined3D", MyVersion.CURRENT_VERSION);
		IJ.wait(20);
		new AboutBox().close();

		IJ.log("=================================================");
		IJ.log("  PER CREARE GLI STACK DA ELABORARE CON QUESTO");
		IJ.log("  PLUGIN SI UTILIZZA 'UNCOMBINED_STACK_COMPOSER'");
		IJ.log("  SIA PER GLI STACK UNCOMBINED CHE COMBINED");
		IJ.log("=================================================");
		UtilAyv.logResizer(600, 400, 400, 400);

		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}
		ImagePlus impUncombined1 = null;
		ImagePlus impUncombined2 = null;
		String pathUncombined1 = null;
		String pathUncombined2 = null;
		String pathCombined = null;
		String[] dir1a = null;
		String[] dir2a = null;
		String dirDefaultUncombined1 = null;
		String dirDefaultUncombined2 = null;

		String dirDefaultCombined = null;
		String dir1 = null;
		String dir2 = null;
		int num1 = 0;
		boolean auto = true;
		if (auto) {
			dirDefaultUncombined1 = Prefs.get("prefer.Unifor3D_dir3", "none");
			DirectoryChooser.setDefaultDirectory(dirDefaultUncombined1);
			DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE PRIMA CARTELLA STACK UNCOMBINED DA ELABORARE");
			dir1 = od1.getDirectory();
			if (dir1 == null)
				return;
			Prefs.set("prefer.Unifor3D_dir3", dir1);
			dir1a = new File(dir1).list();
			num1 = dir1a.length;

			dirDefaultUncombined2 = Prefs.get("prefer.Unifor3D_dir5", "none");
			DirectoryChooser.setDefaultDirectory(dirDefaultUncombined2);
			DirectoryChooser od2 = new DirectoryChooser("SELEZIONARE SECONDA CARTELLA STACK UNCOMBINED DA ELABORARE");
			dir2 = od2.getDirectory();
			if (dir2 == null)
				return;
			Prefs.set("prefer.Unifor3D_dir5", dir2);
			dir2a = new File(dir2).list();
			// num2 = dir2a.length;

			dirDefaultCombined = Prefs.get("prefer.Unifor3D_dir4", "");
			dirDefaultCombined = UtilAyv.dirSeparator(dirDefaultCombined);
			OpenDialog.setDefaultDirectory(dirDefaultCombined);
			OpenDialog dd2 = new OpenDialog("SELEZIONARE LO STACK COMBINED DI RIFERIMENTO");
			pathCombined = dd2.getPath();
			if (pathCombined == null)
				return;
			Prefs.set("prefer.Unifor3D_dir4", pathCombined);
			// num2 = 1;
		} else {
			dirDefaultUncombined1 = Prefs.get("prefer.Unifor3D_dir3", "");
			dirDefaultUncombined1 = UtilAyv.dirSeparator(dirDefaultUncombined1);
			OpenDialog.setDefaultDirectory(dirDefaultUncombined1);
			OpenDialog dd1 = new OpenDialog("SELEZIONARE IL PRIMO STACK UNCOMBINED DA ELABORARE");
			pathUncombined1 = dd1.getPath();
			if (pathUncombined1 == null)
				return;
			Prefs.set("prefer.Unifor3D_dir3", pathUncombined1);
			num1 = 1;
			dirDefaultUncombined2 = Prefs.get("prefer.Unifor3D_dir5", "none");
			dirDefaultUncombined2 = UtilAyv.dirSeparator(dirDefaultUncombined2);
			OpenDialog.setDefaultDirectory(dirDefaultUncombined2);
			OpenDialog dd5 = new OpenDialog("SELEZIONARE IL SECONDO STACK UNCOMBINED DA ELABORARE");
			pathUncombined2 = dd5.getPath();
			if (pathUncombined2 == null)
				return;
			Prefs.set("prefer.Unifor3D_dir5", dir2);

			dirDefaultCombined = Prefs.get("prefer.Unifor3D_dir4", "");
			dirDefaultCombined = UtilAyv.dirSeparator(dirDefaultCombined);
			OpenDialog.setDefaultDirectory(dirDefaultCombined);
			OpenDialog dd2 = new OpenDialog("SELEZIONARE LO STACK COMBINED DI RIFERIMENTO");
			pathCombined = dd2.getPath();
			if (pathCombined == null)
				return;

			Prefs.set("prefer.Unifor3D_dir4", pathCombined);
		}

		IJ.log("dir1= " + dir1);
		IJ.log("dir2= " + dir2);
		IJ.log("pathCombined= " + pathCombined);

		GenericDialog gd = new GenericDialog("", IJ.getInstance());
		int minValue = 1;
		int maxValue = 12;
		int defaultValue = 5;
		gd.addSlider("colori simulata (i livelli di calcolo saranno comunque 12", minValue, maxValue, defaultValue);
		// String[] items = { "5 livelli", "12 livelli" };
		// gd.addRadioButtonGroup("SIMULATE", items, 2, 2, "5 livelli");
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		// String level = gd.getNextRadioButton();
		int livelli = (int) gd.getNextNumber();
		int livelli3 = 12;
		// int livelli = 0;
		// if (level.equals("5 livelli")) {
		// livelli = 5;
		// } else {
		// livelli = 12;
		// }

		int gridWidth = 2;
		int gridHeight = livelli3;
		int gridSize = gridWidth * gridHeight;
		TextField[] tf2 = new TextField[gridSize]; // String[]
		String[] lab2 = { "min% classe 1", "max% classe 1", "min% classe 2", "max% classe 2", "min% classe 3",
				"max% classe 3", "min% classe 4", "max% classe 4", "min% classe 5", "max% classe 5", "min% classe 6",
				"max% classe 6", "min% classe7", "max% classe 7", "min% classe 8", "max% classe 8", "min% classe 9",
				"max% classe 9", "min% classe 10", "max% classe 10", "min% classe 11", "max% classe 11",
				"min% classe 12", "max% classe 12" };
		double[] value2 = new double[gridSize];

		double[] value3 = { 20, 100, 10, 20, -10, 10, -20, -10, -30, -20, -40, -30, -50, -40, -60, -50, -70, -60, -80,
				-70, -90, -80, -100, -90 };

		MyGenericDialogGrid mgdg = new MyGenericDialogGrid();

		for (int i1 = 0; i1 < value2.length; i1++) {
			value2[i1] = mgdg.getValue2(Prefs.get("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, "0"));
		}

		int decimals = 0;
		String title2 = "LIMITI CLASSI PIXELS (sempre 12)";
		if (mgdg.showDialog3(gridWidth, gridHeight, tf2, lab2, value2, value3, title2, decimals)) {
		}

		for (int i1 = 0; i1 < value2.length; i1++) {
			Prefs.set("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, value2[i1]);
		}

		int[] minimi = new int[livelli3];
		int[] massimi = new int[livelli3];
		int conta = 0;
		boolean buco = false;
		for (int i1 = 0; i1 < livelli3; i1++) {
			minimi[i1] = (int) value2[conta++];
			massimi[i1] = (int) value2[conta++];
		}

		for (int i1 = 0; i1 < livelli3 - 1; i1++) {
			if (minimi[i1] != massimi[i1 + 1])
				buco = true;
		}
		if (buco)
			MyLog.waitHere("WHY MAKE AN UGLY HOLE IN YOUR CLASSES ??");

		String[] myLabels = new String[livelli3];
		String sigmin = "";
		String sigmax = "";
		for (int i1 = 0; i1 < livelli3; i1++) {
			if (minimi[i1] < 0) {
				sigmin = "";
			} else {
				sigmin = "+";
			}
			if (massimi[i1] < 0) {
				sigmax = "";
			} else {
				sigmax = "+";
			}

			myLabels[i1] = sigmin + minimi[i1] + " " + sigmax + massimi[i1];
		}

		// =================================================================
		// ELABORAZIONE STACK COMBINED DI RIFERIMENTO
		// =================================================================

		ImagePlus impCombined = UtilAyv.openImageNormal(pathCombined);
		// impCombined.resetDisplayRange();
		impCombined.show();

		ImageStack stackCombined = impCombined.getImageStack();
		if (stackCombined == null) {
			IJ.log("imageFromStack.stackCombined== null");
			return;
		}

		if (stackCombined.getSize() < 2) {
			MyLog.waitHere("Per le elaborazioni 3D ci vuole uno stack di più immagini!");
			return;
		}

		IJ.log("================= ELABORAZIONE STACK COMBINED ================");
		int[] coordinates1 = new int[4];
		coordinates1[0] = impCombined.getWidth() / 2;
		coordinates1[1] = impCombined.getHeight() / 2;
		coordinates1[2] = 0;
		coordinates1[3] = 0;

		double[] sphereA = MySphere.centerSphere(impCombined, demo0);

		IJ.log("centro fantoccio X=" + sphereA[0] + " Y= " + sphereA[1] + " Z= " + sphereA[2] + " diametro= "
				+ sphereA[3]);

		ImagePlus impMapR1 = null;
		ImagePlus impMapG1 = null;
		ImagePlus impMapB1 = null;
		ImagePlus impMapRGB1 = null;
		ImageStack stackRGB1 = null;

		ImagePlus impMapR2 = null;
		ImagePlus impMapG2 = null;
		ImagePlus impMapB2 = null;
		ImagePlus impMapRGB2 = null;
		ImageStack stackRGB2 = null;
		ImagePlus impMapR3 = null;
		ImagePlus impMapG3 = null;
		ImagePlus impMapB3 = null;

		int width = impCombined.getWidth();
		int height = impCombined.getHeight();
		int depth = impCombined.getStackSize();
		int bitdepth = 24;
		int algoColor = 1;

		boolean generate = true;
		if (generate) {
			impMapR1 = MySphere.generaMappazzaVuota16(width, height, depth, "mapR");
			impMapG1 = MySphere.generaMappazzaVuota16(width, height, depth, "mapG");
			impMapB1 = MySphere.generaMappazzaVuota16(width, height, depth, "mapB");
			stackRGB1 = ImageStack.create(width, height, depth, bitdepth);
			impMapRGB1 = new ImagePlus("mapRGB1", stackRGB1);
			impMapR2 = MySphere.generaMappazzaVuota16(width, height, depth, "mapR");
			impMapG2 = MySphere.generaMappazzaVuota16(width, height, depth, "mapG");
			impMapB2 = MySphere.generaMappazzaVuota16(width, height, depth, "mapB");
			stackRGB2 = ImageStack.create(width, height, depth, bitdepth);
			impMapRGB2 = new ImagePlus("mapRGB2", stackRGB2);
			impMapR3 = MySphere.generaMappazzaVuota16(width, height, depth, "mapR");
			impMapG3 = MySphere.generaMappazzaVuota16(width, height, depth, "mapG");
			impMapB3 = MySphere.generaMappazzaVuota16(width, height, depth, "mapB");
			generate = false;
		}

		// =========================
		// SFERA ESTERNA GRIGIA
		// =========================
		int[] colorRGB4 = { 80, 80, 80 };
		boolean surfaceonly = true;
		int[] bounds = new int[3];
		bounds[0] = width;
		bounds[1] = height;
		bounds[2] = depth;
		MySphere.addSphere(impMapR1, impMapG1, impMapB1, sphereA, bounds, colorRGB4, surfaceonly);
		MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, algoColor);
		impMapRGB1.show();

		// =================================================================
		// ELABORAZIONE STACK UNCOMBINED
		// =================================================================

		int count0 = 0;
		int colorCoil = -1;
		long time3 = System.nanoTime();
		IJ.log("================= ELABORAZIONE DI " + num1 + " STACK UNCOMBINED ================");
		impCombined.resetDisplayRange();
		impCombined.repaintWindow();
		while (count0 < num1) {
			long time1 = System.nanoTime();
			int[] vetClassi = null;
			int[] vetClassi3 = null;
			int[] vetTotClassi = new int[livelli3 + 1];

			if (auto) {
				pathUncombined1 = dir1 + dir1a[count0];
				pathUncombined2 = dir2 + dir2a[count0];
				IJ.log("pathUncombined1= " + pathUncombined1);
				IJ.log("pathUncombined2= " + pathUncombined2);
			}
			impUncombined1 = UtilAyv.openImageNoDisplay(pathUncombined1, false);
			if (impUncombined1 == null)
				MyLog.waitHere("uncombined1==null");
			impUncombined2 = UtilAyv.openImageNoDisplay(pathUncombined2, false);
			if (impUncombined2 == null)
				MyLog.waitHere("uncombined2==null");
			String t1 = impUncombined2.getTitle();

			// =========================================================
			// Utilizzando le coordinate sfera trovate sulla immagine combinata
			// estraggo le tre proiezioni di ognuno dei due stack
			// =========================================================
			int crossx = (int) sphereA[0];
			int crossy = (int) sphereA[1];
			int crossz = (int) sphereA[2];
			impUncombined1.show();
			String aux1 = impUncombined1.getTitle();
			aux1 = aux1 + "_1";
			impUncombined1.setTitle(aux1);
			IJ.run(impUncombined1, "Orthogonal Views", "");
			Orthogonal_Views ort1 = Orthogonal_Views.getInstance();
			ort1.setCrossLoc(crossx, crossy, crossz);
			IJ.wait(100);
			ImagePlus imp102 = ort1.getXZImage();
			ImagePlus impXZ1 = new Duplicator().run(imp102);
			IJ.wait(100);
			ImagePlus imp103 = ort1.getYZImage();
			ImagePlus impYZ1 = new Duplicator().run(imp103);
			IJ.wait(100);
			ImagePlus impXY1 = MyStackUtils.imageFromStack(impUncombined1, crossz);
			impXY1.setTitle("XY1");
			Orthogonal_Views.stop();

			impUncombined2.show();
			String aux2 = impUncombined2.getTitle();
			aux2 = aux2 + "_2";
			impUncombined2.setTitle(aux2);
			IJ.run(impUncombined2, "Orthogonal Views", "");
			Orthogonal_Views ort2 = Orthogonal_Views.getInstance();
			ort1.setCrossLoc(crossx, crossy, crossz);
			IJ.wait(100);
			IJ.wait(100);
			ImagePlus imp1022 = ort2.getXZImage();
			ImagePlus impXZ2 = new Duplicator().run(imp1022);
			IJ.wait(100);
			ImageUtils.closeImageWindow(impXZ2);
			ImagePlus imp1032 = ort2.getYZImage();
			ImagePlus impYZ2 = new Duplicator().run(imp1032);
			IJ.wait(100);
			ImagePlus impXY2 = MyStackUtils.imageFromStack(impUncombined2, crossz);
			impXY2.setTitle("XY2");
			Orthogonal_Views.stop();
			ImageUtils.closeImageWindow(impXY2);

			//
			//
			//
			// =========================================================
			// =========================================================

			t1 = t1 + "-2";
			count0++;
			IJ.log("===================================");
			IJ.log("count0= " + count0);
			ImageStack imaStack1 = impUncombined1.getImageStack();
			if (imaStack1 == null) {
				IJ.log("imageFromStack.imaStack== null");
				return;
			}
			if (imaStack1.getSize() < 2) {
				MyLog.waitHere("Per le elaborazioni 3D ci vuole uno stack di piu'mmagini!");
				return;
			}
			int demolevel = 0;
			double[] circularSpot = MySphere.searchCircularSpot(impUncombined1, sphereA, diam11x11, "", demolevel);
			int x2 = (int) circularSpot[0];
			int y2 = (int) circularSpot[1];
			int z2 = (int) circularSpot[2];
			double[] sphereB = new double[4];
			sphereB[0] = x2;
			sphereB[1] = y2;
			sphereB[2] = z2;
			sphereB[3] = diam11x11;
			surfaceonly = false;
			/// volendo fare modifiche ai colori devo solo preoccuparmi di
			/// costruire il corretto colorRGB2, in questo modo devo solo
			/// preoccuparmni delle simulate
			int[] colorRGB2 = new int[3];
			colorCoil++;
			if (colorCoil == MyColor.colori())
				colorCoil = 0;

			colorRGB2 = MyColor.coloreSfera(colorCoil, livelli);
			// }
			MySphere.addSphere(impMapR1, impMapG1, impMapB1, sphereB, bounds, colorRGB2, surfaceonly);
			MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, algoColor);
			impMapRGB1.updateAndDraw();

			double[] sphereC = sphereB.clone();
			sphereC[3] = diam7x7;

			double[] vetpixel_7x7 = MySphere.vectorizeSphericalSpot16(impUncombined1, sphereC, paintPixels);

			int len1 = vetpixel_7x7.length;

			MyLog.logVector(vetpixel_7x7, "vetpixel_7x7 vectorizeSphericalSpot16");

			double sMROI = ArrayUtils.vetMean(vetpixel_7x7);

			double sd_MROI = ArrayUtils.vetSdKnuth(vetpixel_7x7);
			double p_MROI = sd_MROI / Math.sqrt(2.0);

			IJ.log("Dati sfera (xc, yc, zc, radius)= " + sphereC[0] + ", " + sphereC[1] + ", " + sphereC[2] + ", "
					+ sphereC[3]);
			IJ.log("Volume effettivo sfera [voxels] = " + len1 + "[voxels]");
			IJ.log("Mean sfera " + count0 + " = " + sMROI);
			IJ.log("Volume effettivo sfera [voxels] = " + len1 + "[voxels]");
			double[] vetpixel_11x11 = MySphere.vectorizeSphericalSpot16(impUncombined1, sphereB, paintPixels);
			MyLog.logVector(vetpixel_11x11, "vetpixel_11x11 vectorizeSphericalSpot16");

			ImageStack imaStack2 = impUncombined2.getImageStack();
			if (imaStack2 == null) {
				IJ.log("imageFromStack.imaStack== null");
				return;
			}
			if (imaStack2.getSize() < 2) {
				MyLog.waitHere("Per le elaborazioni 3D ci vuole uno stack di piu'mmagini!");
				return;
			}
			ImagePlus impDiff = MyStackUtils.stackDiff(impUncombined1, impUncombined2);
			impDiff.setTitle("IMMAGINE DIFFERENZA");
			if (step) {
				impDiff.show();
				// MyLog.waitHere("immagine differenza");
			}

			double[] vetpixeldiff_11x11 = MySphere.vectorizeSphericalSpot32(impDiff, sphereB, paintPixels);
			MyLog.logVector(vetpixeldiff_11x11, "vetpixeldiff_11x11 vectorizeSphericalSpot32");

			// ============================================
			// INIZIO CALCOLO SNR
			// ============================================
			ArrayList<Double> pixlist_11x11OK = new ArrayList<Double>();
			ArrayList<Double> pixlistdiff_11x11OK = new ArrayList<Double>();

			for (int i1 = 0; i1 < vetpixel_11x11.length; i1++) {
				if (vetpixel_11x11[i1] > (5 * p_MROI)) {
					pixlist_11x11OK.add(vetpixel_11x11[i1]);
					pixlistdiff_11x11OK.add(vetpixeldiff_11x11[i1]);
				}
			}
			double[] vetpixok = ArrayUtils.arrayListToArrayDouble(pixlist_11x11OK);
			double[] vetpixdiffok = ArrayUtils.arrayListToArrayDouble(pixlistdiff_11x11OK);
			MyLog.logVector(vetpixdiffok, "vetpixdiffok");
			IJ.log("pixel_TEST (>121)= " + vetpixok.length);
			double sd_diff = ArrayUtils.vetSdKnuth(vetpixdiffok);
			IJ.log("s_MROI= " + sMROI);
			IJ.log("sd_diff= " + sd_diff);
			// MyLog.waitHere("rumore sfera");
			double snrMROI = 0;
			if (vetpixok.length < 121) {
				MyLog.waitHere("diametro ricerca troppo piccolo");
			} else {
				snrMROI = sMROI * Math.sqrt(2) / sd_diff;
			}
			IJ.log("snrMROI= " + snrMROI);
			String subCoil = ReadDicom.readDicomParameter(impUncombined1, MyConst.DICOM_COIL);
			rt1.incrementCounter();
			rt1.addValue("TIPO ", "ANALISI 3D BOBINA UNCOMBINED");

			rt1.addValue("subCOIL", subCoil);
			rt1.addValue("Fantoccio [x:y:z:d] ", IJ.d2s(sphereA[0], 0) + ":" + IJ.d2s(sphereA[1], 0) + ":"
					+ IJ.d2s(sphereA[2], 0) + ":" + IJ.d2s(sphereA[3], 0));

			rt1.addValue("hotSphere [x:y:z:d] ", IJ.d2s(sphereB[0], 0) + ":" + IJ.d2s(sphereB[1], 0) + ":"
					+ IJ.d2s(sphereB[2], 0) + ":" + IJ.d2s(sphereB[3], 0));

			rt1.addValue("SEGNALE", sMROI);
			rt1.addValue("RUMORE", sd_diff);
			rt1.addValue("SNR", snrMROI);
			// ora devo calcolare l' FWHM del segnale lungo il segmento
			// centro/bordo
			IJ.log("count0= " + count0 + " slice= " + (int) sphereB[1]);
			double[] outFwhm2 = null;
			double[][] profile = MySphere.getProfile3D(impUncombined1, sphereA, sphereB, false);
			IJ.log("lunghezza profilo= " + profile.length);
			double[] prof2 = new double[profile.length];
			for (int i1 = 0; i1 < profile.length; i1++) {
				prof2[i1] = profile[i1][3];
			}
			String codice = "";
			boolean verbose = false;
			double dimPixel = 1;
			outFwhm2 = MyFwhm.analyzeProfile(prof2, dimPixel, codice, false, verbose);
			if (outFwhm2 == null)
				MyLog.waitHere("fwhm2==null");
			rt1.addValue("FWHM ", outFwhm2[0]);
			rt1.addValue("peak/2", outFwhm2[2] / 2);

			// ================================================
			// SIMULATA X IMMAGINE
			// ================================================
			int slice = 0;
			if (true) {
				// impMapR2.show();
				// impMapG2.show();
				// impMapB2.show();
				// impUncombined1.show();
				// =========================
				// SFERA ESTERNA GRIGIA
				// =========================
				int[] colorRGB44 = { 6, 6, 6 };
				surfaceonly = true;
				int[] bounds2 = new int[3];
				bounds2[0] = width;
				bounds2[1] = height;
				bounds2[2] = depth;
				MySphere.addSphere(impMapR2, impMapG2, impMapB2, sphereA, bounds, colorRGB44, surfaceonly);
				MySphere.compilaMappazzaCombinata(impMapR2, impMapG2, impMapB2, impMapRGB2, algoColor);
				impMapRGB2.show();

				debuglevel = 0;
				for (int i1 = 0; i1 < depth; i1++) {
					slice = i1 + 1;
					ImagePlus imp20 = MyStackUtils.imageFromStack(impUncombined1, slice);
					double[] circle = new double[3];
					circle[0] = sphereA[0];
					circle[1] = sphereA[1];
					double diam1 = MySphere.projectedDiameter(sphereA, slice);
					circle[2] = diam1;
					vetClassi = MySphere.simulataGrigio16(sMROI, imp20, circle, impMapR2, impMapG2, impMapB2, slice,
							livelli, minimi, massimi, colorCoil, puntatore, debuglevel, sphereC);
					debuglevel = 0;

					// vetClassi = MySphere.simulataGrigio16(sMROI, imp20,
					// impMapR2, impMapG2, impMapB2, slice, livelli,
					// minimi, massimi, colorCoil, algoColor, puntatore,
					// debuglevel);
					// for (int i2 = 0; i2 < vetClassi.length; i2++) {
					// vetTotClassi[i2] = vetTotClassi[i2] + vetClassi[i2];
					// }
					impMapR2.updateAndDraw();
					impMapG2.updateAndDraw();
					impMapB2.updateAndDraw();
					MySphere.compilaMappazzaCombinata(impMapR2, impMapG2, impMapB2, impMapRGB2, algoColor);
				}
			}

			// ================================================
			// SIMULATA 12 LIVELLI X REPORT
			// ================================================
			int slice1 = 0;
			if (true) {

				debuglevel = 0;
				for (int i1 = 0; i1 < depth; i1++) {
					slice1 = i1 + 1;
					ImagePlus imp20 = MyStackUtils.imageFromStack(impUncombined1, slice1);
					double[] circle = new double[3];
					circle[0] = sphereA[0];
					circle[1] = sphereA[1];
					double diam1 = MySphere.projectedDiameter(sphereA, slice1);
					circle[2] = diam1;
					vetClassi3 = MySphere.simulataGrigio16(sMROI, imp20, circle, impMapR3, impMapG3, impMapB3, slice1,
							livelli3, minimi, massimi, colorCoil, puntatore, debuglevel, sphereC);
					debuglevel = 0;

					// vetClassi = MySphere.simulataGrigio16(sMROI, imp20,
					// impMapR2, impMapG2, impMapB2, slice, livelli,
					// minimi, massimi, colorCoil, algoColor, puntatore,
					// debuglevel);
					for (int i2 = 0; i2 < vetClassi3.length; i2++) {
						vetTotClassi[i2] = vetTotClassi[i2] + vetClassi3[i2];
					}
				}
			}

			long time2 = System.nanoTime();
			String tempo1 = MyTimeUtils.stringNanoTime(time2 - time1);
			IJ.log("Tempo calcolo sfera " + count0 + "   hh:mm:ss.ms " + tempo1);
			impUncombined1.updateAndDraw();
			double totColorati = 0;
			for (int i1 = 0; i1 < vetTotClassi.length - 1; i1++) {
				totColorati = totColorati + vetTotClassi[i1];
			}
			double totFondo = vetTotClassi[vetTotClassi.length - 1];
			rt1.addValue("Voxels sfera colorati", totColorati);
			rt1.addValue("Voxels sfera fondo", totFondo);

			for (int i2 = 0; i2 < minimi.length; i2++) {
				rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2], vetTotClassi[i2]);
			}
			double[] matPercClassi = new double[vetTotClassi.length - 1];
			for (int i1 = 0; i1 < 12; i1++) {
				matPercClassi[i1] = (vetTotClassi[i1] / (totColorati + totFondo)) * 100;
			}
			rt1.addValue("Voxels sfera colorati [%]", totColorati * 100 / (totColorati + totFondo));
			rt1.addValue("Voxels sfera fondo [%]", ResultsTable.d2s(totFondo * 100 / (totColorati + totFondo), 2));

			for (int i2 = 0; i2 < minimi.length; i2++) {
				rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2] + "[%]",
						ResultsTable.d2s(matPercClassi[i2], 2));
			}

			rt1.show("Results");
			// ===========================================================
			// CALCOLO UNIFORMITA' 2D PURE QUI OBVIOUSLY !
			// ===========================================================

			// int direction = 1;
			// double maxFitError = 20.0;
			// double maxBubbleGapLimit = 2.0;
			double profond = 10; // usavamo 30
			// double angle = Double.NaN;

			boolean demo1 = false;
			String[] tit1 = { "ANALISI 2D PROIEZ.XY", "ANALISI 2D PROIEZ.YZ", "ANALISI 2D PROIEZ.XZ" };
			String[] tit2 = { "coloriSimulataXY", "coloriSimulataYZ", "coloriSimulataXZ" };
			// int[][] matClassiDIR = new int[6][2];

			ImagePlus impDIR1 = null;
			ImagePlus impDIR2 = null;
			for (int i1 = 0; i1 < 3; i1++) {
				if (i1 == 0) {
					impDIR1 = impXY1;
					impDIR2 = impXY2;
				}
				if (i1 == 1) {
					impDIR1 = impYZ1;
					impDIR2 = impYZ2;
				}
				if (i1 == 2) {
					impDIR1 = impXZ1;
					impDIR2 = impXZ2;
				}
				impDIR1.show();
				// double[] circleDIR = MySphere.centerCircleCannyEdge(impDIR1,
				// direction, maxFitError, maxBubbleGapLimit,
				// demo1);
				double[] circleDIR = new double[3];
				double width3 = impDIR1.getWidth();
				double height3 = impDIR1.getHeight();
				if (width3 < height3) {
					circleDIR[0] = sphereA[0] - (height3 - width3) / 2;
					circleDIR[1] = sphereA[1];
				} else if (width3 > height3) {
					circleDIR[0] = sphereA[0];
					circleDIR[1] = sphereA[1] - (width3 - height3) / 2;
				} else {
					circleDIR[0] = sphereA[0];
					circleDIR[1] = sphereA[1];
				}

				circleDIR[2] = sphereA[3];
				String info10 = "";
				int mode = 2;
				// ====================================

				// pisquano, serve per cerchio esterno !!!
				// out2[0] = xCenterRoi;
				// out2[1] = yCenterRoi;
				// out2[2] = xCenterCircle;
				// out2[3] = yCenterCircle;
				//
				// out2[4] = xMaxima;
				// out2[5] = yMaxima;
				// out2[6] = angle11;
				// out2[7] = xBordo;
				// out2[8] = yBordo;
				// out2[9] = diamCircle;

				double out2[] = positionSearch55(impDIR1, profond, info10, mode, timeout, circleDIR);

				// ====================================
				Overlay overDIR = new Overlay();
				impDIR1.setOverlay(overDIR);
				impDIR1.setRoi(new OvalRoi(circleDIR[0] - circleDIR[2] / 2, circleDIR[1] - circleDIR[2] / 2,
						circleDIR[2], circleDIR[2]));
				impDIR1.getRoi().setStrokeColor(Color.green);
				overDIR.addElement(impDIR1.getRoi());
				impDIR1.deleteRoi();
				impDIR1.show();
				int diamMROIDIR = (int) Math.round(circleDIR[2] * MyConst.P3_AREA_PERC_80_DIAM);
				impDIR1.setRoi(new OvalRoi(circleDIR[0] - diamMROIDIR / 2, circleDIR[1] - diamMROIDIR / 2, diamMROIDIR,
						diamMROIDIR));
				impDIR1.getRoi().setStrokeColor(Color.red);
				overDIR.addElement(impDIR1.getRoi());
				impDIR1.deleteRoi();
				impDIR1.setRoi(new OvalRoi(circleDIR[0] - diamMROIDIR / 2, circleDIR[1] - diamMROIDIR / 2, diamMROIDIR,
						diamMROIDIR));
				ImageStatistics statDIR = impDIR1.getStatistics();

				// ============================================================================
				// Fine calcoli geometrici
				// Inizio calcoli Uniformita'
				// ============================================================================
				Overlay over2 = new Overlay();
				Overlay over3 = new Overlay();
				// Overlay over4 = new Overlay();

				int sqNEA = MyConst.P10_NEA_11X11_PIXEL;
				// disegno MROI gia' predeterminata
				impDIR1.setOverlay(over2);
				int xCenterRoi = (int) out2[0];
				int yCenterRoi = (int) out2[1];
				int xCenterCircle = (int) out2[2];
				int yCenterCircle = (int) out2[3];

				// int xMaxima = (int) out2[4];
				// int yMaxima = (int) out2[5];
				// angle = out2[6];
				int xBordo = (int) out2[7];
				int yBordo = (int) out2[8];

				// int width1 = impDIR1.getWidth();
				// int height1 = impDIR1.getHeight();

				if (true) {
					// MyLog.waitHere();
					// =================================================
					// Centro cerchio
					MyCircleDetector.drawCenter(impDIR1, over2, xCenterCircle, yCenterCircle, Color.red);

					MyCircleDetector.drawCenter(impDIR1, over2, xBordo, yBordo, Color.pink);

					impDIR1.setRoi(new Line(xCenterCircle, yCenterCircle, xBordo, yBordo));
					over2.addElement(impDIR1.getRoi());
					over2.setStrokeColor(Color.green);

					impDIR1.setRoi(xCenterRoi - sqNEA / 2, yCenterRoi - sqNEA / 2, sqNEA, sqNEA);

					impDIR1.getRoi().setStrokeColor(Color.green);
					impDIR1.getRoi().setStrokeWidth(1.1);
					over2.addElement(impDIR1.getRoi());

					impDIR1.updateAndDraw();
					impDIR1.deleteRoi();

				}
				int sq7 = MyConst.P10_MROI_7X7_PIXEL;
				impDIR1.setRoi(xCenterRoi - sq7 / 2, yCenterRoi - sq7 / 2, sq7, sq7);
				// int indexMROI = 0;
				if (verbose) {
					impDIR1.getRoi().setStrokeColor(Color.red);
					impDIR1.getRoi().setStrokeWidth(1.1);
					over2.addElement(impDIR1.getRoi());
					impDIR1.getRoi().setName("MROI");
					// indexMROI = over2.getIndex("MROI");

				}

				impDIR1.updateAndDraw();
				ImageStatistics statMROI_7x7 = impDIR1.getStatistics();
				Rectangle boundRecMROI_7x7 = impDIR1.getProcessor().getRoi();
				double xCenterMROI_7x7 = boundRecMROI_7x7.getCenterX();
				double yCenterMROI_7x7 = boundRecMROI_7x7.getCenterY();

				double xBkg = impDIR1.getWidth() - MyConst.P10_X_ROI_BACKGROUND;
				double yBkg = MyConst.P10_Y_ROI_BACKGROUND;
				boolean irraggiungibile = false;
				int diamBkg = MyConst.P10_DIAM_ROI_BACKGROUND;
				int guard = 10;
				boolean circle = true;
				int select = 1;
				double[] backPos = UtilAyv.positionSearch15(impDIR1, out2, xBkg, yBkg, diamBkg, guard, select, info10,
						circle, mode, irraggiungibile);

				xBkg = backPos[0] - diamBkg / 2;
				yBkg = backPos[1] - diamBkg / 2;

				impDIR1.setRoi(new OvalRoi((int) xBkg, (int) yBkg, (int) diamBkg, (int) diamBkg));
				ImageStatistics statBkg = impDIR1.getStatistics();

				// if (verbose) {
				ImageUtils.addOverlayRoi(impDIR1, Color.yellow, 1.1);

				ImagePlus impDiffDIR = UtilAyv.genImaDifference(impDIR1, impDIR2);
				impDiffDIR.setTitle("DIFFERENZA");
				// if (verbose && !fast && demo) {
				if (true) {
					impDiffDIR.show();
				}
				impDiffDIR.setOverlay(over3);

				MyCircleDetector.drawCenter(impDiffDIR, over3, xCenterCircle, yCenterCircle, Color.red);

				MyCircleDetector.drawCenter(impDiffDIR, over3, xBordo, yBordo, Color.pink);

				impDIR1.setRoi(new Line(xCenterCircle, yCenterCircle, xBordo, yBordo));
				over3.addElement(impDIR1.getRoi());
				over3.setStrokeColor(Color.green);
				impDiffDIR.killRoi();

				ImageStatistics statImaDiff_MROI_7x7 = impDiffDIR.getStatistics();
				if (impDiffDIR.isVisible())
					ImageUtils.imageToFront(impDiffDIR);

				//
				// calcolo P su imaDiff
				//
				double prelimImageNoiseEstimate_7x7 = statImaDiff_MROI_7x7.stdDev / Math.sqrt(2);

				impDIR1.setRoi(xCenterRoi - sqNEA / 2, yCenterRoi - sqNEA / 2, sqNEA, sqNEA);

				// ==================================================================
				// qui, se il numero dei pixel < 121 dovra' incrementare sqR2 e
				// ripetere il loop
				// ==================================================================

				double checkPixelsLimit = MyConst.P10_CHECK_PIXEL_MULTIPLICATOR * prelimImageNoiseEstimate_7x7;
				int area11x11 = MyConst.P10_NEA_11X11_PIXEL * MyConst.P10_NEA_11X11_PIXEL;

				int enlarge = 0;
				int pixx = 0;

				do {

					// boolean paintPixels = true;

					pixx = countPixOverLimitCentered(impDIR1, xCenterRoi, yCenterRoi, sqNEA, checkPixelsLimit,
							paintPixels, over2);

					impDIR1.setRoi(xCenterRoi - sqNEA / 2, yCenterRoi - sqNEA / 2, sqNEA, sqNEA);
					impDIR1.updateAndDraw();

					if (pixx < area11x11) {
						sqNEA = sqNEA + 2; // accrescimento area
						enlarge = enlarge + 1;
					}
					// if (step)
					// MyLog.waitHere(listaMessaggi(35) + sqNEA, debug);

					// verifico che quando cresce il lato del quadrato non si
					// esca
					// dall'immagine

					if ((xCenterRoi + sqNEA - enlarge) >= width || (xCenterRoi - enlarge) <= 0) {
						MyLog.waitHere("ATTENZIONE la NEA esce dall'immagine senza riuscire a \n"
								+ "trovare 121 pixel che superino il  test il programma \n" + "TERMINA PREMATURAMENTE");
						// return null;
						// broken = true;
						return;
					}
					if ((yCenterRoi + sqNEA - enlarge) >= height || (yCenterRoi - enlarge) <= 0) {
						MyLog.waitHere("ATTENZIONE la NEA esce dall'immagine senza riuscire a \n"
								+ "trovare 121 pixel che superino il  test il programma \n" + "TERMINA PREMATURAMENTE");
						// return null;
						// broken = true;
						return;
					}
					// if (pixx >= area11x11)
					// MyLog.waitHere("Accrescimento NEA riuscito, pixels
					// validi= ");

				} while (pixx < area11x11);

				impDIR1.setRoi(xCenterRoi - sqNEA / 2, yCenterRoi - sqNEA / 2, sqNEA, sqNEA);
				over3.addElement(impDIR1.getRoi());
				over3.setStrokeColor(Color.green);

				impDIR1.updateAndDraw();

				ImageUtils.roiCenter(impDIR1);
				//
				// calcolo SD su imaDiff quando i corrispondenti pixel
				// di imp1 passano il test
				//
				// qui era il problema devStandardNema non era centered e quindi
				// faceva il quadrato spostato

				double[] out11 = devStandardNemaCentered(impDIR1, impDiff, xCenterRoi, yCenterRoi, sqNEA,
						checkPixelsLimit, paintPixels, over2);
				double background = out11[1] / Math.sqrt(2);
				// if (step)
				// MyLog.waitHere("vedi area");

				// MyLog.waitHere("<<<<<<<<<<<<<<<<<< FINE CALCOLO DEVIAZIONE
				// STANDARD NEMA >>>>>>>>>>>>>>>>>>>>>>");

				double finalSnrNEMA = statMROI_7x7.mean / (out11[1] / Math.sqrt(2));

				String simulataName = "SIMULATA";

				double mean = 0;
				boolean test = false;

				ImagePlus impSimulata2 = ImageUtils.generaSimulata12colori(mean, impDIR1, step, verbose, test);

				// int[][] classiSimulata =
				// ImageUtils.generaSimulata12colori(xCenterRoi, yCenterRoi,
				// sq7, impDIR1,
				// simulataName, mode, timeout);
				double[] out3 = ImageUtils.crossingFrame(xCenterRoi, yCenterRoi, xCenterCircle, yCenterCircle, width,
						height);

				double dist1 = MyFwhm.lengthCalculation(out3[0], out3[1], xCenterRoi, yCenterRoi);
				double dist2 = MyFwhm.lengthCalculation(out3[2], out3[3], xCenterRoi, yCenterRoi);
				int xStartProfile = 0;
				int yStartProfile = 0;
				int xEndProfile = 0;
				int yEndProfile = 0;

				if (dist1 <= dist2) {
					xStartProfile = (int) Math.round(out3[0]);
					yStartProfile = (int) Math.round(out3[1]);
					xEndProfile = (int) Math.round(out3[2]);
					yEndProfile = (int) Math.round(out3[3]);
				} else {
					xStartProfile = (int) Math.round(out3[2]);
					yStartProfile = (int) Math.round(out3[3]);
					xEndProfile = (int) Math.round(out3[0]);
					yEndProfile = (int) Math.round(out3[1]);
				}

				if (true) {
					impDIR1.setRoi(new Line(xStartProfile, yStartProfile, xEndProfile, yEndProfile));
					impDIR1.updateAndDraw();
				}

				step = false;
				verbose = step;
				double[] profileDIR2 = getProfile(impDIR1, xStartProfile, yStartProfile, xEndProfile, yEndProfile,
						dimPixel, step);
				double[] outFwhmDIR2 = MyFwhm.analyzeProfile(profileDIR2, dimPixel, codice, step, verbose);

				IJ.wait(MyConst.TEMPO_VISUALIZZ * 10);

				if (step)
					MyLog.waitHere("vedi profilo");

				// ########################################################################################ààà
				// double meanDIR = statDIR.mean;
				// double uiPercDIR = uiPercCalculation(statDIR.max,
				// statDIR.min);
				// // ImagePlus impDiffDIR = UtilAyv.genImaDifference(impDIR1,
				// // impDIR2);
				// Overlay overDiffDIR = new Overlay();
				// overDiffDIR.setStrokeColor(Color.red);
				// impDiffDIR.setOverlay(overDiffDIR);
				// impDiffDIR.setRoi(new OvalRoi(circleDIR[0] - diamMROIDIR / 2,
				// circleDIR[1] - diamMROIDIR / 2,
				// diamMROIDIR, diamMROIDIR));
				// impDiffDIR.getRoi().setStrokeColor(Color.red);
				// overDiffDIR.addElement(impDiffDIR.getRoi());
				// ImageStatistics statImaDiffDIR = impDiffDIR.getStatistics();
				// impDiffDIR.deleteRoi();
				// // double meanImaDiff = statImaDiff.mean;
				// double sd_ImaDiffDIR = statImaDiffDIR.stdDev;
				// double noiseImaDiffDIR = sd_ImaDiffDIR / Math.sqrt(2);
				// double snRatioDIR = Math.sqrt(2) * meanDIR / sd_ImaDiffDIR;
				// ArrayList<Integer> pixListSignalXY1 = new
				// ArrayList<Integer>();
				// pixVectorize1(impDIR1, circleDIR[0], circleDIR[1],
				// diamMROIDIR, pixListSignalXY1);
				// int[] pixListSignalDIR =
				// ArrayUtils.arrayListToArrayInt(pixListSignalXY1);
				// double naadDIR = naadCalculation(pixListSignalDIR);

				// ########################################################################################ààà

				//// risultati analisi 2D
				// ----------------------------------
				rt1.incrementCounter();
				rt1.addValue("TIPO ", tit1[i1]);

				rt1.addValue("subCOIL", subCoil);
				rt1.addValue("Fantoccio [x:y:z:d] ", IJ.d2s(sphereA[0], 0) + ":" + IJ.d2s(sphereA[1], 0) + ":"
						+ IJ.d2s(sphereA[2], 0) + ":" + IJ.d2s(sphereA[3], 0));

				rt1.addValue("hotSphere [x:y:z:d] ", IJ.d2s(statMROI_7x7.roiX, 0) + ":" + IJ.d2s(statMROI_7x7.roiY, 0)
						+ ":" + "___" + ":" + IJ.d2s(sqNEA, 0));

				// rt1.addValue("SEGNALE", meanDIR);
				// rt1.addValue("RUMORE", sd_ImaDiffDIR);
				// rt1.addValue("SNR", snRatioDIR);
				// rt1.addValue("FWHM ", outFwhmDIR2[0]);
				// rt1.addValue("peak/2", outFwhmDIR2[2] / 2);

				rt1.addValue("SEGNALE", statMROI_7x7.mean);
				rt1.addValue("RUMORE", background);
				rt1.addValue("SNR", finalSnrNEMA);
				rt1.addValue("FWHM ", outFwhmDIR2[0]);
				rt1.addValue("peak/2", outFwhmDIR2[2] / 2);
				// rt1.addValue("NAAD", naadDIR);

				// circleDIR[0] = sphereA[0];
				// circleDIR[1] = sphereA[1];

				// mode = 10;
				int[][] matClassiDIR = ImageUtils.generaSimulata12classi((int) circleDIR[0], (int) circleDIR[1],
						(int) circleDIR[2], impDIR1, "", mode, yEndProfile);

				// MyLog.logMatrix(matClassiDIR, "classi simulata");
				// MyLog.waitHere("simulata colori " + minimi.length);

				double totColoratiDIR = 0;
				for (int i2 = 0; i2 < matClassiDIR.length - 1; i2++) {
					totColoratiDIR = totColoratiDIR + matClassiDIR[i2][1];
				}
				double totFondoDIR = matClassiDIR[matClassiDIR.length - 1][1];

				double[] matPercClassiDIR = new double[matClassiDIR.length - 1];
				for (int i2 = 0; i2 < matClassiDIR.length - 1; i2++) {
					matPercClassiDIR[i2] = matClassiDIR[i2][1] * 100 / (totColoratiDIR + totFondoDIR);
				}

				rt1.addValue("Voxels sfera colorati", totColoratiDIR);
				rt1.addValue("Voxels sfera fondo", totFondoDIR);

				// IJ.log("lunghezze");
				// IJ.log("min= " + minimi.length);
				// IJ.log("max= " + massimi.length);
				// IJ.log("matClassiDIR= " + matClassiDIR.length);

				for (int i2 = 0; i2 < minimi.length; i2++) {
					// IJ.log(i2 + "min " + minimi[i2] + " max " + massimi[i2] +
					// " classi " + matClassiDIR[i2][1]);
					rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2], matClassiDIR[i2][1]);
				}
				rt1.addValue("Voxels sfera colorati [%]",
						ResultsTable.d2s(totColoratiDIR * 100 / (totColoratiDIR + totFondoDIR), 2));
				rt1.addValue("Voxels sfera fondo [%]", ResultsTable
						.d2s(matClassiDIR[matClassiDIR.length - 1][1] * 100 / (totColoratiDIR + totFondoDIR), 2));
				for (int i2 = 0; i2 < minimi.length - 1; i2++) {
					rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2] + "[%]",
							ResultsTable.d2s(matPercClassiDIR[i2], 2));
				}

				ImageUtils.closeImageWindow(impDIR1);
				ImageUtils.closeImageWindow(impDiffDIR);
				rt1.show("Results");
			}

			if (WindowManager.getFrame("Profilo penetrazione__") != null) {
				IJ.selectWindow("Profilo penetrazione__");
				IJ.run("Close");
			}
			rt1.show("Results");

			ImageUtils.closeImageWindow(impUncombined1);
			ImageUtils.closeImageWindow(impUncombined2);
			ImageUtils.closeImageWindow(impDiff);
			//
			// ImageWindow iw1 = impUncombined1.getWindow();
			// if (iw1 != null)
			// iw1.close();
		}
		int[] colorRGB5 = { 24, 24, 24 };

		MySphere.addSphereFilling(impMapR1, impMapG1, impMapB1, sphereA, bounds, colorRGB5);
		MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, algoColor);

		long time4 = System.nanoTime();
		String tempo2 = MyTimeUtils.stringNanoTime(time4 - time3);
		IJ.log("Tempo totale  hh:mm:ss.ms " + tempo2);
		MyLog.waitHere("FINE");
	} // chiude

	public static double[] positionSearch(ImagePlus imp11, double profond, String info1, int mode, int timeout) {

		boolean demo = false;
		Color colore1 = Color.red;
		Color colore2 = Color.green;
		Color colore3 = Color.red;

		if (mode == 10 || mode == 3)
			demo = true;
		// ================================================================================
		// Inizio calcoli geometrici
		// ================================================================================
		//
		boolean manual = false;
		double ax = 0;
		double ay = 0;
		int xCenterCircle = 0;
		int yCenterCircle = 0;
		int diamCircle = 0;
		double xMaxima = 0;
		double yMaxima = 0;
		double angle11 = 0;
		double xCenterRoi = 0;
		double yCenterRoi = 0;
		double maxFitError = 30;
		Overlay over12 = new Overlay();
		if (imp11 == null)
			MyLog.waitHere("imp11==null");

		double dimPixel = 1;
		ImageWindow iw11 = null;
		if (demo)
			iw11 = imp11.getWindow();

		int width = imp11.getWidth();
		int height = imp11.getHeight();
		ImagePlus imp12 = imp11.duplicate();
		imp12.setTitle("DUP");
		// -------------------------------------------------
		// Determinazione del cerchio
		// -------------------------------------------------
		ImageProcessor ip12 = imp12.getProcessor();
		RankFilters rk1 = new RankFilters();
		double radius = 0.1;
		int filterType = RankFilters.VARIANCE;
		rk1.rank(ip12, radius, filterType);
		imp12.updateAndDraw();
		double max1 = imp12.getStatistics().max;
		ip12.subtract(max1 / 30);
		imp12.updateAndDraw();
		imp12.setOverlay(over12);
		double[][] myPeaks = new double[4][1];
		int[] myXpoints = new int[16];
		int[] myYpoints = new int[16];
		int[] xcoord = new int[2];
		int[] ycoord = new int[2];
		boolean manualOverride = false;
		int[] vetx0 = new int[8];
		int[] vetx1 = new int[8];
		int[] vety0 = new int[8];
		int[] vety1 = new int[8];

		vetx0[0] = 0;
		vety0[0] = height / 2;
		vetx1[0] = width;
		vety1[0] = height / 2;
		// ----
		vetx0[1] = width / 2;
		vety0[1] = 0;
		vetx1[1] = width / 2;
		vety1[1] = height;
		// ----
		vetx0[2] = 0;
		vety0[2] = 0;
		vetx1[2] = width;
		vety1[2] = height;
		// -----
		vetx0[3] = width;
		vety0[3] = 0;
		vetx1[3] = 0;
		vety1[3] = height;
		// -----
		vetx0[4] = width / 4;
		vety0[4] = 0;
		vetx1[4] = width * 3 / 4;
		vety1[4] = height;
		// ----
		vetx0[5] = width * 3 / 4;
		vety0[5] = 0;
		vetx1[5] = width / 4;
		vety1[5] = height;
		// ----
		vetx0[6] = width;
		vety0[6] = height * 1 / 4;
		vetx1[6] = 0;
		vety1[6] = height * 3 / 4;
		// ----
		vetx0[7] = 0;
		vety0[7] = height * 1 / 4;
		vetx1[7] = width;
		vety1[7] = height * 3 / 4;

		String[] vetTitle = { "orizzontale", "verticale", "diagonale sinistra", "diagonale destra", "inclinata 1",
				"inclinata 2", "inclinata 3", "inclinata 4" };

		// multipurpose line analyzer

		int count = -1;

		int[] xPoints3 = null;
		int[] yPoints3 = null;
		boolean vertical = false;
		boolean valido = true;
		for (int i1 = 0; i1 < 8; i1++) {
			xcoord[0] = vetx0[i1];
			ycoord[0] = vety0[i1];
			xcoord[1] = vetx1[i1];
			ycoord[1] = vety1[i1];
			imp12.setRoi(new Line(xcoord[0], ycoord[0], xcoord[1], ycoord[1]));
			if (demo) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			if (i1 == 1)
				vertical = true;
			else
				vertical = false;

			boolean showProfiles = false;

			if (demo && i1 == 0)
				showProfiles = true;

			myPeaks = profileAnalyzer(imp12, dimPixel, vetTitle[i1], showProfiles, vertical, timeout);

			valido = true;
			// String direction1 = ReadDicom.readDicomParameter(imp11,
			// MyConst.DICOM_IMAGE_ORIENTATION);
			// String direction2 = "1\0\0\01\0";

			if (myPeaks != null) {

				// per evitare le bolle d'aria escluderò il punto in alto per
				// l'immagine assiale ed il punto a sinistra dell'immagine
				// sagittale. Considero punto in alto quello con coordinata y <
				// mat/2 e come punto a sinistra quello con coordinata x < mat/2

				for (int i2 = 0; i2 < myPeaks[0].length; i2++) {

					// if ((direction1.compareTo("0\\1\\0\\0\\0\\-1") == 0) &&
					// (i1 == 0)) {
					// if (((int) (myPeaks[0][i2]) < width / 2)) {
					// valido = false;
					// } else
					// ;
					// }
					//
					// if ((direction1.compareTo("1\\0\\0\\1\\0") == 0) && (i1
					// == 1)) {
					// if (((int) (myPeaks[1][i2]) < height / 2)) {
					// valido = false;
					// } else
					// ;
					// }

					if (valido) {
						count++;
						myXpoints[count] = (int) (myPeaks[0][i2]);
						myYpoints[count] = (int) (myPeaks[1][i2]);
						ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[0][i2]), (int) (myPeaks[1][i2]), colore1);
						imp12.updateAndDraw();
						ImageUtils.imageToFront(imp12);
					}
				}
			}

			// devo compattare i vettori myXpoints e myYpoints, ovviamente a
			// patto che count >=0;
		}

		if (count >= 0) {
			count++;
			xPoints3 = new int[count];
			yPoints3 = new int[count];

			for (int i3 = 0; i3 < count; i3++) {
				xPoints3[i3] = myXpoints[i3];
				yPoints3[i3] = myYpoints[i3];
			}
		} else {
			xPoints3 = null;
			yPoints3 = null;
		}
		// qui di seguito pulisco l'overlay, dovrò preoccuparmi di ridisegnare i
		// punti
		imp12.deleteRoi();
		over12.clear();
		imp12.updateAndDraw();

		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------

		if (xPoints3 == null || xPoints3.length < 3) {
			UtilAyv.showImageMaximized(imp11);

			// MyLog.waitHere(listaMessaggi(19), debug);
			manual = true;
		}

		if (!manual) {
			// reimposto i punti trovati
			PointRoi pr12 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
			pr12.setPointType(2);
			pr12.setSize(4);
			imp12.setRoi(pr12);

			if (demo) {
				// ridisegno i punti sull'overlay
				imp12.getRoi().setStrokeColor(colore1);
				over12.addElement(imp12.getRoi());
				imp12.setOverlay(over12);
			}
			// ---------------------------------------------------
			// eseguo ora fitCircle per trovare centro e dimensione del
			// fantoccio
			// ---------------------------------------------------
			ImageUtils.fitCircle(imp12);
			if (demo) {
				imp12.getRoi().setStrokeColor(colore3);
				over12.addElement(imp12.getRoi());
			}

			Rectangle boundRec = imp12.getProcessor().getRoi();
			xCenterCircle = Math.round(boundRec.x + boundRec.width / 2);
			yCenterCircle = Math.round(boundRec.y + boundRec.height / 2);
			diamCircle = boundRec.width;
			if (!manualOverride)
				writeStoredRoiData(boundRec);
			MyCircleDetector.drawCenter(imp12, over12, xCenterCircle, yCenterCircle, colore3);
			// ----------------------------------------------------------
			// Misuro l'errore sul fit rispetto ai punti imposti
			// -----------------------------------------------------------
			double[] vetDist = new double[xPoints3.length];
			double sumError = 0;
			for (int i1 = 0; i1 < xPoints3.length; i1++) {
				vetDist[i1] = ImageUtils.pointCirconferenceDistance(xPoints3[i1], yPoints3[i1], xCenterCircle,
						yCenterCircle, diamCircle / 2);
				sumError += Math.abs(vetDist[i1]);
			}
			if (sumError > maxFitError) {
				// MyLog.waitHere("maxFitError");
				// -------------------------------------------------------------
				// disegno il cerchio ed i punti, in modo da date un feedback
				// grafico al messaggio di eccessivo errore nel fit
				// -------------------------------------------------------------
				UtilAyv.showImageMaximized(imp12);
				over12.remove(pr12);
				imp12.setOverlay(over12);
				imp12.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2, yCenterCircle - diamCircle / 2, diamCircle,
						diamCircle));
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				PointRoi pr1 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
				pr1.setSize(4);
				pr1.setPointType(1);
				imp12.setRoi(pr1);
				imp12.getRoi().setStrokeColor(Color.red);
				over12.addElement(imp12.getRoi());
				imp12.deleteRoi();
				manual = true;
			}

		}

		Rectangle boundRec = null;
		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------
		if (xPoints3 != null && xPoints3.length >= 3 && !manual) {
			imp12.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
			ImageUtils.fitCircle(imp12);
			if (demo) {
				over12.addElement(imp12.getRoi());
				over12.setStrokeColor(Color.red);
			}
			boundRec = imp12.getProcessor().getRoi();

		} else {

			// NON SI SONO DETERMINATI 3 PUNTI DEL CERCHIO, SELEZIONE MANUALE

			if (!imp11.isVisible())
				UtilAyv.showImageMaximized(imp11);
			imp11.setRoi(new OvalRoi((width / 2) - 100, (height / 2) - 100, 180, 180));
			imp11.updateAndDraw();
			imp11.getRoi().setStrokeColor(Color.red);
			imp11.getRoi().setStrokeWidth(1.1);
			MyLog.waitHere(
					"imp11= " + imp11.getTitle() + "\nNon si riescono a determinare le coordinate corrette del cerchio"
							+ "\nRichiesto ridimensionamennto e riposizionamento della ROI indicata in rosso, attorno al fantoccio\n"
							+ "POI premere  OK");

			boundRec = imp11.getProcessor().getRoi();
			xCenterCircle = boundRec.x + boundRec.width / 2;
			yCenterCircle = boundRec.y + boundRec.height / 2;
			diamCircle = boundRec.width;

			imp11.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2, yCenterCircle - diamCircle / 2, diamCircle,
					diamCircle));
			imp11.updateAndDraw();
			imp11.getRoi().setStrokeColor(Color.green);
			imp11.getRoi().setStrokeWidth(0);
			over12.clear();
			over12.add(imp11.getRoi());
			//
			// Ho cosi' risolto la mancata localizzazione automatica del
			// fantoccio (messaggi non visualizzati in junit)
			//
		}

		// ==========================================================================
		// ==========================================================================
		// porto in primo piano l'immagine originale
		ImageUtils.imageToFront(iw11);
		// ==========================================================================
		// ==========================================================================
		imp11.setOverlay(over12);
		imp12.close();
		xCenterCircle = boundRec.x + boundRec.width / 2;
		yCenterCircle = boundRec.y + boundRec.height / 2;
		diamCircle = boundRec.width;
		MyCircleDetector.drawCenter(imp11, over12, xCenterCircle, yCenterCircle, Color.red);

		// ----------------------------------------------------------
		// disegno la ROI del maxima, a solo scopo dimostrativo !
		// ----------------------------------------------------------
		//

		// x1 ed y1 sono le due coordinate del punto di maxima
		double[] out10 = MyFilter.maxPosition11x11(imp11);
		xMaxima = out10[0];
		yMaxima = out10[1];

		imp12.killRoi();

		// ===============================================================
		// intersezioni retta - circonferenza
		// ===============================================================

		double[] out11 = ImageUtils.getCircleLineCrossingPoints(xCenterCircle, yCenterCircle, xMaxima, yMaxima,
				xCenterCircle, yCenterCircle, diamCircle / 2);

		// il punto che ci interesasa sara' quello con minor distanza dal
		// maxima
		double dx1 = xMaxima - out11[0];
		double dx2 = xMaxima - out11[2];
		double dy1 = yMaxima - out11[1];
		double dy2 = yMaxima - out11[3];
		double lun1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
		double lun2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

		double xBordo = 0;
		double yBordo = 0;
		if (lun1 < lun2) {
			xBordo = out11[0];
			yBordo = out11[1];
		} else {
			xBordo = out11[2];
			yBordo = out11[3];
		}

		if (demo)
			MyCircleDetector.drawCenter(imp11, over12, (int) xBordo, (int) yBordo, Color.pink);

		//
		// -----------------------------------------------------------
		// Calcolo delle effettive coordinate del segmento
		// centro-circonferenza
		// ----------------------------------------------------------
		//
		double xStartRefLine = (double) xCenterCircle;
		double yStartRefLine = (double) yCenterCircle;
		double xEndRefLine = xBordo;
		double yEndRefLine = yBordo;

		imp11.setRoi(new Line(xCenterCircle, yCenterCircle, (int) xBordo, (int) yBordo));
		angle11 = imp11.getRoi().getAngle(xCenterCircle, yCenterCircle, (int) xBordo, (int) yBordo);

		over12.addElement(imp11.getRoi());
		over12.setStrokeColor(Color.red);
		//
		// -----------------------------------------------------------
		// Calcolo coordinate centro della MROI
		// ----------------------------------------------------------
		//

		double[] out1 = interpolaProfondCentroROI(xEndRefLine, yEndRefLine, xStartRefLine, yStartRefLine,
				profond / dimPixel);
		ax = out1[0];
		ay = out1[1];

		int sqNEA = MyConst.P10_NEA_11X11_PIXEL;

		imp11.setRoi((int) ax - sqNEA / 2, (int) ay - sqNEA / 2, sqNEA, sqNEA);
		imp11.updateAndDraw();
		over12.addElement(imp11.getRoi());
		over12.setStrokeColor(Color.red);

		//
		// Se non necessito di un intervento manuale, mi limito a leggere le
		// coordinate della ROI determinata in automatico.
		//

		Rectangle boundRec4 = imp11.getProcessor().getRoi();
		xCenterRoi = boundRec4.getCenterX();
		yCenterRoi = boundRec4.getCenterY();
		imp12.hide();

		// MyLog.waitHere("ax= " + ax + " ay= " + ay + " xCenterRoi= "
		// + xCenterRoi + " yCenterRoi= " + yCenterRoi);
		// }

		if (demo && manual) {

			ImageUtils.imageToFront(iw11);

			// UtilAyv.showImageMaximized(imp11);
			imp11.setOverlay(over12);
			imp11.setRoi((int) ax - sqNEA / 2, (int) ay - sqNEA / 2, sqNEA, sqNEA);
			imp11.updateAndDraw();
			// if (demo)
			MyLog.waitHere(info1 + "\n \nMODIFICA MANUALE POSIZIONE ROI", true, timeout);
			//
			// Vado a rileggere solo le coordinate della ROI, quelle del
			// cerchio,
			// del punto di maxima e dell'angolo resteranno quelle determinate
			// in
			// precedenza (anche perche' non vengono comunque piu' utilizzate
			// per
			// i
			// calcoli)
			//
			Rectangle boundRec3 = imp11.getProcessor().getRoi();
			xCenterRoi = boundRec3.getCenterX();
			yCenterRoi = boundRec3.getCenterY();

		}

		double[] out2 = new double[10];
		out2[0] = xCenterRoi;
		out2[1] = yCenterRoi;
		out2[2] = xCenterCircle;
		out2[3] = yCenterCircle;

		out2[4] = xMaxima;
		out2[5] = yMaxima;
		out2[6] = angle11;
		out2[7] = xBordo;
		out2[8] = yBordo;
		out2[9] = diamCircle;
		return out2;
	}

	public static double[] positionSearch55(ImagePlus imp11, double profond, String info1, int mode, int timeout,
			double[] circle) {

		boolean demo = false;
		Color colore1 = Color.red;
		Color colore2 = Color.green;
		Color colore3 = Color.red;

		if (mode == 10 || mode == 3)
			demo = true;
		// ================================================================================
		// Inizio calcoli geometrici
		// ================================================================================
		//
		boolean manual = false;
		double ax = 0;
		double ay = 0;
		int xCenterCircle = 0;
		int yCenterCircle = 0;
		int diamCircle = 0;
		double xMaxima = 0;
		double yMaxima = 0;
		double angle11 = 0;
		double xCenterRoi = 0;
		double yCenterRoi = 0;
		double maxFitError = 30;
		Overlay over12 = new Overlay();
		if (imp11 == null)
			MyLog.waitHere("imp11==null");

		double dimPixel = 1;
		ImageWindow iw11 = null;
		if (demo)
			iw11 = imp11.getWindow();

		int width = imp11.getWidth();
		int height = imp11.getHeight();
		ImagePlus imp12 = imp11.duplicate();
		imp12.setTitle("DUP");
		// -------------------------------------------------
		// Determinazione del cerchio
		// -------------------------------------------------
		// ImageProcessor ip12 = imp12.getProcessor();
		// RankFilters rk1 = new RankFilters();
		// double radius = 0.1;
		// int filterType = RankFilters.VARIANCE;
		// rk1.rank(ip12, radius, filterType);
		// imp12.updateAndDraw();
		// double max1 = imp12.getStatistics().max;
		// ip12.subtract(max1 / 30);
		// imp12.updateAndDraw();
		// imp12.setOverlay(over12);
		// double[][] myPeaks = new double[4][1];
		// int[] myXpoints = new int[16];
		// int[] myYpoints = new int[16];
		// int[] xcoord = new int[2];
		// int[] ycoord = new int[2];
		// boolean manualOverride = false;
		// int[] vetx0 = new int[8];
		// int[] vetx1 = new int[8];
		// int[] vety0 = new int[8];
		// int[] vety1 = new int[8];
		//
		// vetx0[0] = 0;
		// vety0[0] = height / 2;
		// vetx1[0] = width;
		// vety1[0] = height / 2;
		// // ----
		// vetx0[1] = width / 2;
		// vety0[1] = 0;
		// vetx1[1] = width / 2;
		// vety1[1] = height;
		// // ----
		// vetx0[2] = 0;
		// vety0[2] = 0;
		// vetx1[2] = width;
		// vety1[2] = height;
		// // -----
		// vetx0[3] = width;
		// vety0[3] = 0;
		// vetx1[3] = 0;
		// vety1[3] = height;
		// // -----
		// vetx0[4] = width / 4;
		// vety0[4] = 0;
		// vetx1[4] = width * 3 / 4;
		// vety1[4] = height;
		// // ----
		// vetx0[5] = width * 3 / 4;
		// vety0[5] = 0;
		// vetx1[5] = width / 4;
		// vety1[5] = height;
		// // ----
		// vetx0[6] = width;
		// vety0[6] = height * 1 / 4;
		// vetx1[6] = 0;
		// vety1[6] = height * 3 / 4;
		// // ----
		// vetx0[7] = 0;
		// vety0[7] = height * 1 / 4;
		// vetx1[7] = width;
		// vety1[7] = height * 3 / 4;
		//
		// String[] vetTitle = { "orizzontale", "verticale", "diagonale
		// sinistra", "diagonale destra", "inclinata 1",
		// "inclinata 2", "inclinata 3", "inclinata 4" };
		//
		// // multipurpose line analyzer
		//
		// int count = -1;
		//
		// int[] xPoints3 = null;
		// int[] yPoints3 = null;
		// boolean vertical = false;
		// boolean valido = true;
		// for (int i1 = 0; i1 < 8; i1++) {
		// xcoord[0] = vetx0[i1];
		// ycoord[0] = vety0[i1];
		// xcoord[1] = vetx1[i1];
		// ycoord[1] = vety1[i1];
		// imp12.setRoi(new Line(xcoord[0], ycoord[0], xcoord[1], ycoord[1]));
		// if (demo) {
		// imp12.getRoi().setStrokeColor(colore2);
		// over12.addElement(imp12.getRoi());
		// imp12.updateAndDraw();
		// }
		// if (i1 == 1)
		// vertical = true;
		// else
		// vertical = false;
		//
		// boolean showProfiles = false;
		//
		// if (demo && i1 == 0)
		// showProfiles = true;
		//
		// myPeaks = profileAnalyzer(imp12, dimPixel, vetTitle[i1],
		// showProfiles, vertical, timeout);
		//
		// valido = true;
		// // String direction1 = ReadDicom.readDicomParameter(imp11,
		// // MyConst.DICOM_IMAGE_ORIENTATION);
		// // String direction2 = "1\0\0\01\0";
		//
		// if (myPeaks != null) {
		//
		// // per evitare le bolle d'aria escluderò il punto in alto per
		// // l'immagine assiale ed il punto a sinistra dell'immagine
		// // sagittale. Considero punto in alto quello con coordinata y <
		// // mat/2 e come punto a sinistra quello con coordinata x < mat/2
		//
		// for (int i2 = 0; i2 < myPeaks[0].length; i2++) {
		//
		// // if ((direction1.compareTo("0\\1\\0\\0\\0\\-1") == 0) &&
		// // (i1 == 0)) {
		// // if (((int) (myPeaks[0][i2]) < width / 2)) {
		// // valido = false;
		// // } else
		// // ;
		// // }
		// //
		// // if ((direction1.compareTo("1\\0\\0\\1\\0") == 0) && (i1
		// // == 1)) {
		// // if (((int) (myPeaks[1][i2]) < height / 2)) {
		// // valido = false;
		// // } else
		// // ;
		// // }
		//
		// if (valido) {
		// count++;
		// myXpoints[count] = (int) (myPeaks[0][i2]);
		// myYpoints[count] = (int) (myPeaks[1][i2]);
		// ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[0][i2]), (int)
		// (myPeaks[1][i2]), colore1);
		// imp12.updateAndDraw();
		// ImageUtils.imageToFront(imp12);
		// }
		// }
		// }
		//
		// // devo compattare i vettori myXpoints e myYpoints, ovviamente a
		// // patto che count >=0;
		// }
		//
		// if (count >= 0) {
		// count++;
		// xPoints3 = new int[count];
		// yPoints3 = new int[count];
		//
		// for (int i3 = 0; i3 < count; i3++) {
		// xPoints3[i3] = myXpoints[i3];
		// yPoints3[i3] = myYpoints[i3];
		// }
		// } else {
		// xPoints3 = null;
		// yPoints3 = null;
		// }
		// // qui di seguito pulisco l'overlay, dovrò preoccuparmi di
		// ridisegnare i
		// // punti
		// imp12.deleteRoi();
		// over12.clear();
		// imp12.updateAndDraw();
		//
		// //
		// ----------------------------------------------------------------------
		// // Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// // selezione manuale del cerchio
		// //
		// -------------------------------------------------------------------
		//
		// if (xPoints3 == null || xPoints3.length < 3) {
		// UtilAyv.showImageMaximized(imp11);
		//
		// // MyLog.waitHere(listaMessaggi(19), debug);
		// manual = true;
		// }
		//
		// if (!manual) {
		// // reimposto i punti trovati
		// PointRoi pr12 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
		// pr12.setPointType(2);
		// pr12.setSize(4);
		// imp12.setRoi(pr12);
		//
		// if (demo) {
		// // ridisegno i punti sull'overlay
		// imp12.getRoi().setStrokeColor(colore1);
		// over12.addElement(imp12.getRoi());
		// imp12.setOverlay(over12);
		// }
		// // ---------------------------------------------------
		// // eseguo ora fitCircle per trovare centro e dimensione del
		// // fantoccio
		// // ---------------------------------------------------
		// ImageUtils.fitCircle(imp12);
		// if (demo) {
		// imp12.getRoi().setStrokeColor(colore3);
		// over12.addElement(imp12.getRoi());
		// }
		//
		// Rectangle boundRec = imp12.getProcessor().getRoi();
		// xCenterCircle = Math.round(boundRec.x + boundRec.width / 2);
		// yCenterCircle = Math.round(boundRec.y + boundRec.height / 2);
		// diamCircle = boundRec.width;
		// if (!manualOverride)
		// writeStoredRoiData(boundRec);
		// MyCircleDetector.drawCenter(imp12, over12, xCenterCircle,
		// yCenterCircle, colore3);
		// // ----------------------------------------------------------
		// // Misuro l'errore sul fit rispetto ai punti imposti
		// // -----------------------------------------------------------
		// double[] vetDist = new double[xPoints3.length];
		// double sumError = 0;
		// for (int i1 = 0; i1 < xPoints3.length; i1++) {
		// vetDist[i1] = ImageUtils.pointCirconferenceDistance(xPoints3[i1],
		// yPoints3[i1], xCenterCircle,
		// yCenterCircle, diamCircle / 2);
		// sumError += Math.abs(vetDist[i1]);
		// }
		// if (sumError > maxFitError) {
		// // MyLog.waitHere("maxFitError");
		// // -------------------------------------------------------------
		// // disegno il cerchio ed i punti, in modo da date un feedback
		// // grafico al messaggio di eccessivo errore nel fit
		// // -------------------------------------------------------------
		// UtilAyv.showImageMaximized(imp12);
		// over12.remove(pr12);
		// imp12.setOverlay(over12);
		// imp12.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2,
		// yCenterCircle - diamCircle / 2, diamCircle,
		// diamCircle));
		// imp12.getRoi().setStrokeColor(colore2);
		// over12.addElement(imp12.getRoi());
		// PointRoi pr1 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
		// pr1.setSize(4);
		// pr1.setPointType(1);
		// imp12.setRoi(pr1);
		// imp12.getRoi().setStrokeColor(Color.red);
		// over12.addElement(imp12.getRoi());
		// imp12.deleteRoi();
		// manual = true;
		// }
		//
		// }
		//
		// Rectangle boundRec = null;
		// //
		// ----------------------------------------------------------------------
		// // Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// // selezione manuale del cerchio
		// //
		// -------------------------------------------------------------------
		// if (xPoints3 != null && xPoints3.length >= 3 && !manual) {
		// imp12.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
		// ImageUtils.fitCircle(imp12);
		// if (demo) {
		// over12.addElement(imp12.getRoi());
		// over12.setStrokeColor(Color.red);
		// }
		// boundRec = imp12.getProcessor().getRoi();
		//
		// } else {
		//
		// // NON SI SONO DETERMINATI 3 PUNTI DEL CERCHIO, SELEZIONE MANUALE
		//
		// if (!imp11.isVisible())
		// UtilAyv.showImageMaximized(imp11);
		// imp11.setRoi(new OvalRoi((width / 2) - 100, (height / 2) - 100, 180,
		// 180));
		// imp11.updateAndDraw();
		// imp11.getRoi().setStrokeColor(Color.red);
		// imp11.getRoi().setStrokeWidth(1.1);
		// MyLog.waitHere(
		// "imp11= " + imp11.getTitle() + "\nNon si riescono a determinare le
		// coordinate corrette del cerchio"
		// + "\nRichiesto ridimensionamennto e riposizionamento della ROI
		// indicata in rosso, attorno al fantoccio\n"
		// + "POI premere OK");
		//
		// boundRec = imp11.getProcessor().getRoi();
		// xCenterCircle = boundRec.x + boundRec.width / 2;
		// yCenterCircle = boundRec.y + boundRec.height / 2;
		// diamCircle = boundRec.width;
		//
		// imp11.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2,
		// yCenterCircle - diamCircle / 2, diamCircle,
		// diamCircle));
		// imp11.updateAndDraw();
		// imp11.getRoi().setStrokeColor(Color.green);
		// imp11.getRoi().setStrokeWidth(0);
		// over12.clear();
		// over12.add(imp11.getRoi());
		// //
		// // Ho cosi' risolto la mancata localizzazione automatica del
		// // fantoccio (messaggi non visualizzati in junit)
		// //
		// }

		// ==========================================================================
		// ==========================================================================
		// porto in primo piano l'immagine originale
		ImageUtils.imageToFront(iw11);
		// ==========================================================================
		// ==========================================================================
		imp11.setOverlay(over12);
		imp12.close();
		xCenterCircle = (int) circle[0];
		yCenterCircle = (int) circle[1];
		diamCircle = (int) circle[2];
		MyCircleDetector.drawCenter(imp11, over12, xCenterCircle, yCenterCircle, Color.red);

		// ----------------------------------------------------------
		// disegno la ROI del maxima, a solo scopo dimostrativo !
		// ----------------------------------------------------------
		//

		// x1 ed y1 sono le due coordinate del punto di maxima
		double[] out10 = MyFilter.maxPosition11x11(imp11);
		xMaxima = out10[0];
		yMaxima = out10[1];

		imp12.killRoi();

		// ===============================================================
		// intersezioni retta - circonferenza
		// ===============================================================

		double[] out11 = ImageUtils.getCircleLineCrossingPoints(xCenterCircle, yCenterCircle, xMaxima, yMaxima,
				xCenterCircle, yCenterCircle, diamCircle / 2);

		// il punto che ci interesasa sara' quello con minor distanza dal
		// maxima
		double dx1 = xMaxima - out11[0];
		double dx2 = xMaxima - out11[2];
		double dy1 = yMaxima - out11[1];
		double dy2 = yMaxima - out11[3];
		double lun1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
		double lun2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

		double xBordo = 0;
		double yBordo = 0;
		if (lun1 < lun2) {
			xBordo = out11[0];
			yBordo = out11[1];
		} else {
			xBordo = out11[2];
			yBordo = out11[3];
		}

		if (demo)
			MyCircleDetector.drawCenter(imp11, over12, (int) xBordo, (int) yBordo, Color.pink);

		//
		// -----------------------------------------------------------
		// Calcolo delle effettive coordinate del segmento
		// centro-circonferenza
		// ----------------------------------------------------------
		//
		double xStartRefLine = (double) xCenterCircle;
		double yStartRefLine = (double) yCenterCircle;
		double xEndRefLine = xBordo;
		double yEndRefLine = yBordo;

		imp11.setRoi(new Line(xCenterCircle, yCenterCircle, (int) xBordo, (int) yBordo));
		angle11 = imp11.getRoi().getAngle(xCenterCircle, yCenterCircle, (int) xBordo, (int) yBordo);

		over12.addElement(imp11.getRoi());
		over12.setStrokeColor(Color.red);
		//
		// -----------------------------------------------------------
		// Calcolo coordinate centro della MROI
		// ----------------------------------------------------------
		//

		double[] out1 = interpolaProfondCentroROI(xEndRefLine, yEndRefLine, xStartRefLine, yStartRefLine,
				profond / dimPixel);
		ax = out1[0];
		ay = out1[1];

		int sqNEA = MyConst.P10_NEA_11X11_PIXEL;

		imp11.setRoi((int) ax - sqNEA / 2, (int) ay - sqNEA / 2, sqNEA, sqNEA);
		imp11.updateAndDraw();
		over12.addElement(imp11.getRoi());
		over12.setStrokeColor(Color.red);

		//
		// Se non necessito di un intervento manuale, mi limito a leggere le
		// coordinate della ROI determinata in automatico.
		//

		Rectangle boundRec4 = imp11.getProcessor().getRoi();
		xCenterRoi = boundRec4.getCenterX();
		yCenterRoi = boundRec4.getCenterY();
		imp12.hide();

		// MyLog.waitHere("ax= " + ax + " ay= " + ay + " xCenterRoi= "
		// + xCenterRoi + " yCenterRoi= " + yCenterRoi);
		// }

		if (demo && manual) {

			ImageUtils.imageToFront(iw11);

			// UtilAyv.showImageMaximized(imp11);
			imp11.setOverlay(over12);
			imp11.setRoi((int) ax - sqNEA / 2, (int) ay - sqNEA / 2, sqNEA, sqNEA);
			imp11.updateAndDraw();
			// if (demo)
			MyLog.waitHere(info1 + "\n \nMODIFICA MANUALE POSIZIONE ROI", true, timeout);
			//
			// Vado a rileggere solo le coordinate della ROI, quelle del
			// cerchio,
			// del punto di maxima e dell'angolo resteranno quelle determinate
			// in
			// precedenza (anche perche' non vengono comunque piu' utilizzate
			// per
			// i
			// calcoli)
			//
			Rectangle boundRec3 = imp11.getProcessor().getRoi();
			xCenterRoi = boundRec3.getCenterX();
			yCenterRoi = boundRec3.getCenterY();

		}

		double[] out2 = new double[10];
		out2[0] = xCenterRoi;
		out2[1] = yCenterRoi;
		out2[2] = xCenterCircle;
		out2[3] = yCenterCircle;

		out2[4] = xMaxima;
		out2[5] = yMaxima;
		out2[6] = angle11;
		out2[7] = xBordo;
		out2[8] = yBordo;
		out2[9] = diamCircle;
		return out2;
	}

	public static double[][] profileAnalyzer(ImagePlus imp1, double dimPixel, String title, boolean showProfiles,
			boolean vertical, int timeout) {

		double[][] profi3 = MyLine.decomposer(imp1);
		double[] vetz = new double[profi3[0].length];
		for (int i1 = 0; i1 < profi3[0].length; i1++) {
			vetz[i1] = profi3[2][i1];
		}
		ArrayList<ArrayList<Double>> matOut = null;
		double[][] peaks1 = null;
		double limit = 100;
		do {
			matOut = ImageUtils.peakDet1(profi3, limit);
			peaks1 = new InputOutput().fromArrayListToDoubleTable(matOut);
			if (peaks1 == null) {
				return null;
			}

			if (peaks1.length == 0) {
				return null;
			}
			if (peaks1[0].length == 0) {
				return null;
			}
			if (peaks1[0].length > 2)
				limit = limit + limit * 0.1;
		} while (peaks1[0].length > 2);

		double[] xPoints = new double[peaks1[0].length];
		double[] yPoints = new double[peaks1[0].length];
		double[] zPoints = new double[peaks1[0].length];
		for (int i1 = 0; i1 < peaks1[0].length; i1++) {
			xPoints[i1] = peaks1[0][i1];
			yPoints[i1] = peaks1[1][i1];
			zPoints[i1] = peaks1[2][i1];
		}

		if (showProfiles) {
			Plot plot2 = MyPlot.basePlot2(profi3, title, Color.GREEN, vertical);
			plot2.draw();
			plot2.setColor(Color.red);
			if (vertical)
				plot2.addPoints(yPoints, zPoints, PlotWindow.CIRCLE);
			else
				plot2.addPoints(xPoints, zPoints, PlotWindow.CIRCLE);
			plot2.show();

			Frame lw = WindowManager.getFrame(title);
			if (lw != null)
				lw.setLocation(10, 10);
			MyLog.waitHere();
		}
		if (WindowManager.getFrame(title) != null) {
			IJ.selectWindow(title);
			IJ.run("Close");
		}
		return peaks1;
	}

	public static void writeStoredRoiData(Rectangle boundingRectangle) {

		Prefs.set("prefer.p10rmnDiamFantoc", Integer.toString(boundingRectangle.width));
		Prefs.set("prefer.p10rmnXRoi1", Integer.toString(boundingRectangle.x));
		Prefs.set("prefer.p10rmnYRoi1", Integer.toString(boundingRectangle.y));
	}

	public static double[] interpolaProfondCentroROI(double ax, double ay, double bx, double by, double prof) {

		double ang1 = angoloRad(ax, ay, bx, by);

		double cx = 0;
		double cy = 0;
		// IJ.log("proiezioneX= " + prof * (Math.cos(ang1)));
		// IJ.log("proiezioneY= " + prof * (Math.sin(ang1)));
		cx = ax - prof * (Math.cos(ang1));
		cy = ay + prof * (Math.sin(ang1));
		// IJ.log("cx= " + IJ.d2s(cx) + " cy= " + IJ.d2s(cy));
		double[] out = new double[2];
		out[0] = cx;
		out[1] = cy;

		// MyLog.waitHere("ax=" + ax + " ay=" + ay + " bx=" + bx + " by=" + by
		// + " prof=" + prof + "cx=" + cx + " cy=" + cy);
		return out;
	}

	public static double angoloRad(double ax, double ay, double bx, double by) {

		double dx = ax - bx;
		double dy = by - ay; // dy e' all'incontrario, per le coordinate di
								// ImageJ
		double theta = Math.atan2(dy, dx);
		return theta;

	}

	/**
	 * Calculation of Integral Uniformity Percentual
	 * 
	 * @param max
	 *            max signal
	 * @param min
	 *            min signal
	 * @return
	 */
	public static double uiPercCalculation(double max, double min) {
		// Ui% = ( 1 - ( signalMax - signalMin ) / ( signalMax +
		// signalMin )) * 100
		double uiPerc = (1 - (max - min) / (max + min)) * 100;
		return uiPerc;
	}

	/***
	 * pixVectorize1 lavora sulle immagini costituite da interi
	 * 
	 * @param imp11
	 * @param xCenterMROI
	 * @param yCenterMROI
	 * @param diamMROI
	 * @param pixList11
	 */
	public static void pixVectorize1(ImagePlus imp11, double xCenterMROI, double yCenterMROI, double diamMROI,
			ArrayList<Integer> pixList11) {

		imp11.setRoi(new OvalRoi(xCenterMROI - diamMROI / 2, yCenterMROI - diamMROI / 2, diamMROI, diamMROI));
		Roi roi11 = imp11.getRoi();

		ImageProcessor ip11 = imp11.getProcessor();
		ImageProcessor mask11 = roi11 != null ? roi11.getMask() : null;
		Rectangle r11 = roi11 != null ? roi11.getBounds() : new Rectangle(0, 0, ip11.getWidth(), ip11.getHeight());
		for (int y = 0; y < r11.height; y++) {
			for (int x = 0; x < r11.width; x++) {
				if (mask11 == null || mask11.getPixel(x, y) != 0) {
					pixList11.add((int) ip11.getPixelValue(x + r11.x, y + r11.y));
				}
			}
		}
	}

	/***
	 * pixVectorize1 lavora sulle immagini costituite da float
	 * 
	 * @param imp11
	 * @param xCenterMROI
	 * @param yCenterMROI
	 * @param diamMROI
	 * @param pixList11
	 */

	public static void pixVectorize2(ImagePlus imp11, double xCenterMROI, double yCenterMROI, double diamMROI,
			ArrayList<Float> pixList11) {

		imp11.setRoi(new OvalRoi(xCenterMROI - diamMROI / 2, yCenterMROI - diamMROI / 2, diamMROI, diamMROI));
		Roi roi11 = imp11.getRoi();

		ImageProcessor ip11 = imp11.getProcessor();
		ImageProcessor mask11 = roi11 != null ? roi11.getMask() : null;
		Rectangle r11 = roi11 != null ? roi11.getBounds() : new Rectangle(0, 0, ip11.getWidth(), ip11.getHeight());
		for (int y = 0; y < r11.height; y++) {
			for (int x = 0; x < r11.width; x++) {
				if (mask11 == null || mask11.getPixel(x, y) != 0) {
					pixList11.add((float) ip11.getPixelValue(x + r11.x, y + r11.y));
				}
			}
		}
	}

	public static double naadCalculation(int[] pixListSignal) {

		double mean1 = ArrayUtils.vetMean(pixListSignal);
		// MyLog.waitHere("mean1= "+mean1);
		double val = 0;
		double sum1 = 0;
		for (int i1 = 0; i1 < pixListSignal.length; i1++) {
			val = Math.abs(pixListSignal[i1] - mean1);
			sum1 = sum1 + val;
		}
		// MyLog.waitHere("sum1= "+sum1);
		double result = sum1 / (mean1 * pixListSignal.length);
		return result;
	}

	public static int countPixOverLimitCentered(ImagePlus imp1, int sqX, int sqY, int sqR, double limit,
			boolean paintPixels, Overlay over1) {
		int offset = 0;
		int w = 0;
		int count1 = 0;

		if (imp1 == null) {
			IJ.error("CountPixTest ricevuto null");
			return (0);
		}
		// MyLog.waitHere("sqX= "+sqX+" sqY= "+sqY+" sqR= "+sqR);
		int width = imp1.getWidth();
		short[] pixels1 = UtilAyv.truePixels(imp1);

		boolean ok = false;
		for (int y1 = sqY - sqR / 2; y1 <= (sqY + sqR / 2); y1++) {
			offset = y1 * width;
			for (int x1 = sqX - sqR / 2; x1 <= (sqX + sqR / 2); x1++) {
				w = offset + x1;
				ok = false;

				if (w >= 0 && w < pixels1.length && pixels1[w] > limit) {
					ok = true;
					count1++;
				} else
					ok = false;
				if (paintPixels)
					setOverlayPixel(over1, imp1, x1, y1, Color.green, Color.red, ok);
			}
		}
		return count1;
	}

	public static void setOverlayPixel(Overlay over1, ImagePlus imp1, int x1, int y1, Color col1, Color col2,
			boolean ok) {
		imp1.setRoi(x1, y1, 1, 1);
		if (ok) {
			imp1.getRoi().setStrokeColor(col1);
			imp1.getRoi().setFillColor(col1);
		} else {
			imp1.getRoi().setStrokeColor(col1);
			imp1.getRoi().setFillColor(col2);
		}
		over1.addElement(imp1.getRoi());
		imp1.deleteRoi();
	}

	private static double[] devStandardNemaCentered(ImagePlus imp1, ImagePlus imp3, int sqX1, int sqY1, int sqR,
			double limit, boolean paintPixels, Overlay over1) {
		double[] results = new double[2];
		double value4 = 0.0;
		double sumValues = 0.0;
		double sumSquare = 0.0;
		boolean ok;

		// modifica del 260216
		int sqX = sqX1 - sqR / 2;
		int sqY = sqY1 - sqR / 2;
		// --------

		if ((imp1 == null) || (imp3 == null)) {
			IJ.error("devStandardNema ricevuto null");
			return (null);
		}

		int width = imp1.getWidth();
		short[] pixels1 = UtilAyv.truePixels(imp1);
		int pixelCount = 0;
		int offset = 0;
		ImageProcessor ip3 = imp3.getProcessor();
		float[] pixels4 = (float[]) ip3.getPixels();
		for (int y1 = sqY; y1 < (sqY + sqR); y1++) {
			for (int x1 = sqX; x1 < (sqX + sqR); x1++) {
				ok = false;
				offset = y1 * width + x1;
				// IJ.log("offset= " + offset + " y1= " + y1 + " width= " +
				// width
				// + " x1= " + x1);
				if (pixels1[offset] > limit) {
					pixelCount++;
					value4 = pixels4[offset];
					sumValues += value4;
					sumSquare += value4 * value4;
					ok = true;
				}
				// modifica del 260216
				if (paintPixels)
					setOverlayPixel(over1, imp1, x1, y1, Color.yellow, Color.red, ok);
				// --------

			}
		}

		results[0] = sumValues / pixelCount;
		double sd1 = calculateStdDev4(pixelCount, sumValues, sumSquare);
		results[1] = sd1;
		return (results);
	}

	private static double calculateStdDev4(int num, double sum, double sum2) {
		double sd1;
		if (num > 0) {
			sd1 = (num * sum2 - sum * sum) / num;
			if (sd1 > 0.0)
				sd1 = Math.sqrt(sd1 / (num - 1.0));
			else
				sd1 = 0.0;
		} else
			sd1 = 0.0;
		return (sd1);
	}

	private static double[] getProfile(ImagePlus imp1, int ax, int ay, int bx, int by, double dimPixel, boolean step) {

		if (imp1 == null) {
			IJ.error("getProfile  ricevuto null");
			return (null);
		}
		imp1.setRoi(new Line(ax, ay, bx, by));
		Roi roi1 = imp1.getRoi();
		imp1.killRoi();
		double[] profi1 = ((Line) roi1).getPixels(); // profilo non mediato
		profi1[profi1.length - 1] = 0; // azzero a mano l'ultimo pixel
		if (step) {
			imp1.updateAndDraw();
			ButtonMessages.ModelessMsg("Profilo non mediato  <50>", "CONTINUA");
		}
		return (profi1);
	}

} // ultima
