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
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import utils.AboutBox;
import utils.ArrayUtils;
import utils.ButtonMessages;
import utils.ImageUtils;
import utils.InputOutput;
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
		int diam7x7 = 16;
		int diam11x11 = 20;
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
		String[] items = { "5 livelli", "12 livelli" };
		gd.addRadioButtonGroup("SIMULATE", items, 2, 2, "5 livelli");
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		String level = gd.getNextRadioButton();
		int livelli = 0;
		if (level.equals("5 livelli")) {
			livelli = 5;
		} else {
			livelli = 12;
		}

		int gridWidth = 2;
		int gridHeight = livelli;
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
		String title2 = "LIMITI CLASSI PIXELS";
		if (mgdg.showDialog3(gridWidth, gridHeight, tf2, lab2, value2, value3, title2, decimals)) {
		}

		for (int i1 = 0; i1 < value2.length; i1++) {
			Prefs.set("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, value2[i1]);
		}

		int[] minimi = new int[livelli];
		int[] massimi = new int[livelli];
		int conta = 0;
		boolean buco = false;
		for (int i1 = 0; i1 < livelli; i1++) {
			minimi[i1] = (int) value2[conta++];
			massimi[i1] = (int) value2[conta++];
		}

		for (int i1 = 0; i1 < livelli - 1; i1++) {
			if (minimi[i1] != massimi[i1 + 1])
				buco = true;
		}
		if (buco)
			MyLog.waitHere("WHY MAKE AN UGLY HOLE IN YOUR CLASSES ??");

		int[][] myColor = new int[12][3];
		myColor[0][0] = 255;
		myColor[0][1] = 0;
		myColor[0][2] = 0;
		myColor[1][0] = 255;
		myColor[1][1] = 128;
		myColor[1][2] = 0;
		myColor[2][0] = 255;
		myColor[2][1] = 255;
		myColor[2][2] = 0;
		myColor[3][0] = 128;
		myColor[3][1] = 255;
		myColor[3][2] = 0;
		myColor[4][0] = 0;
		myColor[4][1] = 255;
		myColor[4][2] = 255;
		myColor[5][0] = 0;
		myColor[5][1] = 0;
		myColor[5][2] = 255;
		myColor[6][0] = 127;
		myColor[6][1] = 0;
		myColor[6][2] = 255;
		myColor[7][0] = 255;
		myColor[7][1] = 0;
		myColor[7][2] = 127;

		String[] myLabels = new String[livelli];
		String sigmin = "";
		String sigmax = "";
		for (int i1 = 0; i1 < livelli; i1++) {
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

		int width = impCombined.getWidth();
		int height = impCombined.getHeight();
		int depth = impCombined.getStackSize();
		int bitdepth = 24;
		int algoColor = 3;

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
			generate = false;
		}

		// =========================
		// SFERA ESTERNA GRIGIA
		// =========================
		int[] colorRGB3 = { 100, 100, 100 };
		int[] colorRGB4 = { 20, 20, 20 };
		boolean surfaceonly = true;
		int[] bounds = new int[3];
		bounds[0] = width;
		bounds[1] = height;
		bounds[2] = depth;
		MySphere.addSphere(impMapR1, impMapG1, impMapB1, sphereA, bounds, colorRGB3, surfaceonly);
		MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, algoColor);
		impMapRGB1.show();
		impMapRGB2.show();

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
			int[] vetTotClassi = new int[livelli + 1];

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
			for (int i1 = 0; i1 < 8; i1++) {
				colorRGB2[0] = myColor[colorCoil][0];
				colorRGB2[1] = myColor[colorCoil][1];
				colorRGB2[2] = myColor[colorCoil][2];
			}
			if (colorCoil == 3)
				colorCoil = 0;

			MySphere.addSphere(impMapR1, impMapG1, impMapB1, sphereB, bounds, colorRGB2, surfaceonly);
			MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, algoColor);
			// impMapR1.updateAndDraw();
			// impMapG1.updateAndDraw();
			// impMapB1.updateAndDraw();
			impMapRGB1.updateAndDraw();

			double[] sphereC = sphereB.clone();
			sphereC[3] = diam7x7;

			double[] vetpixel_7x7 = MySphere.vectorizeSphericalSpot(impUncombined1, sphereA, sphereC);
			int len1 = vetpixel_7x7.length;
			double sMROI = ArrayUtils.vetMean(vetpixel_7x7);

			double sd_MROI = ArrayUtils.vetSdKnuth(vetpixel_7x7);
			double p_MROI = sd_MROI / Math.sqrt(2.0);

			IJ.log("Dati sfera (xc, yc, zc, radius)= " + sphereC[0] + ", " + sphereC[1] + ", " + sphereC[2] + ", "
					+ sphereC[3]);
			IJ.log("Volume effettivo sfera [voxels] = " + len1 + "[voxels]");
			IJ.log("Mean sfera " + count0 + " = " + sMROI);
			IJ.log("Volume effettivo sfera [voxels] = " + len1 + "[voxels]");

			double[] vetpixel_11x11 = MySphere.vectorizeSphericalSpot(impUncombined1, sphereA, sphereB);

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

			double[] vetpixeldiff_11x11 = MySphere.vectorizeSphericalSpot(impDiff, sphereA, sphereB);

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
			IJ.log("pixel_TEST (>121)= " + vetpixok.length);
			double sd_diff = ArrayUtils.vetSdKnuth(vetpixdiffok);
			IJ.log("s_MROI= " + sMROI);
			IJ.log("sd_diff= " + sd_diff);
			double snrMROI = 0;
			if (vetpixok.length < 121) {
				MyLog.waitHere("diametro ricerca troppo piccolo");
			} else {
				snrMROI = sMROI * Math.sqrt(2) / sd_diff;
			}
			IJ.log("snrMROI= " + snrMROI);

			String subCoil = ReadDicom.readDicomParameter(impUncombined1, MyConst.DICOM_COIL);

			rt1.incrementCounter();

			rt1.addValue("subCOIL", subCoil);

			rt1.addValue("Fantoccio [x:y:z:d] ", IJ.d2s(sphereA[0], 0) + ":" + IJ.d2s(sphereA[1], 0) + ":"
					+ IJ.d2s(sphereA[2], 0) + ":" + IJ.d2s(sphereA[3], 0));

			rt1.addValue("hotSphere [x:y:z:d] ", IJ.d2s(sphereB[0], 0) + ":" + IJ.d2s(sphereB[1], 0) + ":"
					+ IJ.d2s(sphereB[2], 0) + ":" + IJ.d2s(sphereB[3], 0));

			rt1.addValue("SEGNALE_mroi", sMROI);
			rt1.addValue("RUMORE_diff", sd_diff);
			rt1.addValue("SNR_mroi", snrMROI);
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
			// SIMULATE
			// ================================================
			int slice = 0;
			if (true) {
				impMapR2.show();
				impMapG2.show();
				impMapB2.show();
				for (int i1 = 0; i1 < depth; i1++) {
					slice = i1 + 1;
					ImagePlus imp20 = MyStackUtils.imageFromStack(impUncombined1, slice);
					vetClassi = MySphere.simulataGrigio16(sMROI, imp20, impMapR2, impMapG2, impMapB2, slice, livelli,
							minimi, massimi, colorCoil, algoColor, puntatore, debuglevel);
					for (int i2 = 0; i2 < vetClassi.length; i2++) {
						vetTotClassi[i2] = vetTotClassi[i2] + vetClassi[i2];
					}
					impMapR2.updateAndDraw();
					impMapG2.updateAndDraw();
					impMapB2.updateAndDraw();
					MySphere.compilaMappazzaCombinata(impMapR2, impMapG2, impMapB2, impMapRGB2, algoColor);
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

			// rt1.addValue("Voxels fondo ", vetTotClassi[vetTotClassi.length -
			// 1]);

			double[] matPercClassi = new double[vetTotClassi.length - 1];
			for (int i1 = 0; i1 < vetTotClassi.length - 1; i1++) {
				matPercClassi[i1] = (vetTotClassi[i1] / (totColorati + totFondo)) * 100;
			}

			rt1.addValue("Voxels sfera colorati [%]", totColorati * 100 / (totColorati + totFondo));
			rt1.addValue("Voxels sfera fondo [%]", ResultsTable.d2s(totFondo * 100 / (totColorati + totFondo), 2));

			for (int i2 = 0; i2 < minimi.length; i2++) {
				rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2] + "[%]",
						ResultsTable.d2s(matPercClassi[i2], 2));
			}

			rt1.show("Results");

			if (WindowManager.getFrame("Profilo penetrazione__") != null) {
				IJ.selectWindow("Profilo penetrazione__");
				IJ.run("Close");
			}
			ImageWindow iw1 = impUncombined1.getWindow();
			if (iw1 != null)
				iw1.close();
		}
		MySphere.addSphereFilling(impMapR1, impMapG1, impMapB1, sphereA, bounds, colorRGB4, false);
		MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, algoColor);

		long time4 = System.nanoTime();
		String tempo2 = MyTimeUtils.stringNanoTime(time4 - time3);
		IJ.log("Tempo totale  hh:mm:ss.ms " + tempo2);

		MyLog.waitHere("FINE");

	} // chiude

} // ultima
