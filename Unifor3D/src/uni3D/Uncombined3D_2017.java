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

		// boolean debug = false;
		// if (debuglevel > 0)
		// debug = true;
		ResultsTable rt1 = ResultsTable.getResultsTable();

		new AboutBox().about("Uncombined3D", MyVersion.CURRENT_VERSION);
		IJ.wait(20);
		new AboutBox().close();

		GenericDialog gd = new GenericDialog("", IJ.getInstance());
		String[] items = { "5 livelli", "12 livelli" };
		gd.addRadioButtonGroup("SIMULATE", items, 2, 2, "5 livelli");
		// gd.addCheckbox("auto", true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		String level = gd.getNextRadioButton();
		// boolean auto = gd.getNextBoolean();
		boolean twelve;
		int livelli = 0;
		if (level.equals("5 livelli")) {
			twelve = false;
			livelli = 5;
		} else {
			twelve = true;
			livelli = 12;
		}

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
		if (mgdg.showDialog3(gridWidth, gridHeight, tf2, lab2, value2, value3, title2, decimals)) { // comodo
																									// il
																									// preset
																									// ????
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

		/// IMMAGINI SIMULATE
		int[] myColor = new int[livelli];
		if (livelli == 5) {
			myColor[0] = ((255 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
			myColor[1] = ((255 & 0xff) << 16) | ((165 & 0xff) << 8) | (0 & 0xff);
			myColor[2] = ((255 & 0xff) << 16) | ((255 & 0xff) << 8) | (0 & 0xff);
			myColor[3] = ((124 & 0xff) << 16) | ((252 & 0xff) << 8) | (50 & 0xff);
			myColor[4] = ((0 & 0xff) << 16) | ((128 & 0xff) << 8) | (0 & 0xff);
		} else {
			myColor[0] = ((25 & 0xff) << 16) | ((25 & 0xff) << 8) | (112 & 0xff);
			myColor[1] = ((0 & 0xff) << 16) | ((0 & 0xff) << 8) | (205 & 0xff);
			myColor[2] = ((138 & 0xff) << 16) | ((43 & 0xff) << 8) | (226 & 0xff);
			myColor[3] = ((0 & 0xff) << 16) | ((100 & 0xff) << 8) | (0 & 0xff);
			myColor[4] = ((0 & 0xff) << 16) | ((128 & 0xff) << 8) | (0 & 0xff);
			myColor[5] = ((50 & 0xff) << 16) | ((205 & 0xff) << 8) | (50 & 0xff);
			myColor[6] = ((128 & 0xff) << 16) | ((128 & 0xff) << 8) | (0 & 0xff);
			myColor[7] = ((255 & 0xff) << 16) | ((255 & 0xff) << 8) | (0 & 0xff);
			myColor[8] = ((255 & 0xff) << 16) | ((165 & 0xff) << 8) | (0 & 0xff);
			myColor[9] = ((250 & 0xff) << 16) | ((128 & 0xff) << 8) | (114 & 0xff);
			myColor[10] = ((255 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
			myColor[11] = ((0 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
		}

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
		int myColors = 3;

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
		int[] colorRGB3 = { 90, 90, 90 };
		int[] colorRGB4 = { 10, 10, 10 };
		boolean surfaceonly = false;
		int[] bounds = new int[3];
		bounds[0] = width;
		bounds[1] = height;
		bounds[2] = depth;
		MySphere.addSphere(impMapR1, impMapG1, impMapB1, sphereA, bounds, colorRGB3, surfaceonly);
		MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, myColors);
		MySphere.addSphere(impMapR2, impMapG2, impMapB2, sphereA, bounds, colorRGB4, true);
		MySphere.compilaMappazzaCombinata(impMapR2, impMapG2, impMapB2, impMapRGB2, myColors);
		impMapRGB1.show();
		impMapRGB2.show();

		// =================================================================
		// ELABORAZIONE STACK UNCOMBINED
		// =================================================================

		int count0 = 0;
		int cr = 0;
		int cg = 0;
		int cb = 0;
		int colorCoil = 0;
		long time3 = System.nanoTime();
		int r1 = 250;
		int g1 = 250;
		int b1 = 0;
		int r2 = 0;
		int g2 = 250;
		int b2 = 250;
		int r3 = 250;
		int g3 = 0;
		int b3 = 250;
		IJ.log("================= ELABORAZIONE DI " + num1 + " STACK UNCOMBINED ================");

		while (count0 < num1) {

			long time1 = System.nanoTime();

			colorCoil += 1;
			if (colorCoil == 1) {
				cr = r1;
				cg = g1;
				cb = b1;
			}
			if (colorCoil == 2) {
				cr = r2;
				cg = g2;
				cb = b2;
			}
			if (colorCoil == 3) {
				cr = r3;
				cg = g3;
				cb = b3;
				colorCoil = 0;
			}
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
			double[] circularSpot = MySphere.searchCircularSpot(impUncombined1, sphereA, diam7x7, "", demolevel);

			int x2 = (int) circularSpot[0];
			int y2 = (int) circularSpot[1];
			int z2 = (int) circularSpot[2];
			int[] colorRGB2 = new int[3];
			colorRGB2[0] = cr * 2;
			colorRGB2[1] = cg * 3;
			colorRGB2[2] = cb;
			double[] sphereB = new double[4];
			sphereB[0] = x2;
			sphereB[1] = y2;
			sphereB[2] = z2;
			sphereB[3] = diam7x7;

			surfaceonly = false;
			MySphere.addSphere(impMapR1, impMapG1, impMapB1, sphereB, bounds, colorRGB2, surfaceonly);
			MySphere.compilaMappazzaCombinata(impMapR1, impMapG1, impMapB1, impMapRGB1, myColors);
			impMapR1.updateAndDraw();
			impMapG1.updateAndDraw();
			impMapB1.updateAndDraw();
			impMapRGB1.updateAndDraw();

			double[] vetpixel_7x7 = MySphere.vectorizeSphericalSpot(impUncombined1, sphereA, sphereB);
			int len1 = vetpixel_7x7.length;
			double sMROI = ArrayUtils.vetMean(vetpixel_7x7);

			double sd_MROI = ArrayUtils.vetSdKnuth(vetpixel_7x7);
			double p_MROI = sd_MROI / Math.sqrt(2.0);

			IJ.log("Dati sfera (xc, yc, zc, radius)= " + sphereB[0] + ", " + sphereB[1] + ", " + sphereB[2] + ", "
					+ sphereB[3]);
			IJ.log("Volume effettivo sfera [voxels] = " + len1 + "[voxels]");
			IJ.log("Mean sfera " + count0 + " = " + sMROI);
			IJ.log("Volume effettivo sfera [voxels] = " + len1 + "[voxels]");

			double[] sphereC = new double[4];
			sphereC[0] = x2;
			sphereC[1] = y2;
			sphereC[2] = z2;
			sphereC[3] = diam11x11;

			double[] vetpixel_11x11 = MySphere.vectorizeSphericalSpot(impUncombined1, sphereA, sphereC);

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

			double[] vetpixeldiff_11x11 = MySphere.vectorizeSphericalSpot(impDiff, sphereA, sphereC);

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

			rt1.addValue("Fantoccio [x,y,z,d] ", IJ.d2s(sphereA[0], 0) + ", " + IJ.d2s(sphereA[1], 0) + ", "
					+ IJ.d2s(sphereA[2], 0) + ", " + IJ.d2s(sphereA[3], 0));

			rt1.addValue("hotSphere [x,y,z,d] ", IJ.d2s(sphereB[0], 0) + ", " + IJ.d2s(sphereB[1], 0) + ", "
					+ IJ.d2s(sphereB[2], 0) + ", " + IJ.d2s(sphereB[3], 0));

			rt1.addValue("SEGNALE_mroi", sMROI);
			rt1.addValue("RUMORE_diff", sd_diff);
			rt1.addValue("SNR_mroi", snrMROI);

			double[] cross = ImageUtils.getCircleLineCrossingPoints(sphereA[0], sphereA[1], sphereB[0], sphereB[1],
					sphereB[0], sphereB[1], sphereB[3] / 2);

			// il punto che ci interesasa sara' quello con minor distanza dal
			// centro sfera B
			double dx1 = sphereB[0] - cross[0];
			double dx2 = sphereB[0] - cross[2];
			double dy1 = sphereB[1] - cross[1];
			double dy2 = sphereB[1] - cross[3];
			double lun1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
			double lun2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

			double xBordo = 0;
			double yBordo = 0;
			if (lun1 < lun2) {
				xBordo = cross[0];
				yBordo = cross[1];
			} else {
				xBordo = cross[2];
				yBordo = cross[3];
			}

			// ora devo calcolare l' FWHM del segnale lungo il segmento
			// centro/bordo

			IJ.log("count0= " + count0 + " slice= " + (int) sphereB[1]);
			ImagePlus impThis = MyStackUtils.imageFromStack(impUncombined1, (int) sphereB[1]);
			if (impThis == null)
				MyLog.waitHere("impThis==null");

			double dimPixel = ReadDicom.readDouble(
					ReadDicom.readSubstring(ReadDicom.readDicomParameter(impThis, MyConst.DICOM_PIXEL_SPACING), 2));

			double[] out3 = ImageUtils.crossingFrame(sphereA[0], sphereA[1], sphereB[0], sphereB[1], width, height);

			double dist1 = MyFwhm.lengthCalculation(out3[0], out3[1], sphereA[0], sphereA[1]);
			double dist2 = MyFwhm.lengthCalculation(out3[2], out3[3], sphereA[0], sphereA[1]);
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

			double[] profile = getProfile(impThis, xStartProfile, yStartProfile, xEndProfile, yEndProfile, dimPixel,
					false);
			MyLog.logVector(profile, "profile");
			String codice = "";
			boolean verbose = false;
			double[] outFwhm2 = MyFwhm.analyzeProfile(profile, dimPixel, codice, false, verbose);

			MyLog.logVector(outFwhm2, "outFwhm2");

			rt1.addValue("FWHM ", outFwhm2[0]);

			// ================================================
			// SIMULATE
			// ================================================
			int slice = 0;
			for (int i1 = 0; i1 < depth; i1++) {
				slice = i1 + 1;
				ImagePlus imp20 = MyStackUtils.imageFromStack(impUncombined1, slice);

				MySphere.simulataGrigio16(sMROI, imp20, impMapR2, impMapG2, impMapB2, slice, livelli, minimi, massimi,
						colorCoil, myColors, puntatore, debuglevel);
				MySphere.compilaMappazzaCombinata(impMapR2, impMapG2, impMapB2, impMapRGB2, myColors);
			}
			long time2 = System.nanoTime();
			String tempo1 = MyTimeUtils.stringNanoTime(time2 - time1);
			IJ.log("Tempo calcolo sfera " + count0 + "   hh:mm:ss.ms " + tempo1);
			rt1.show("Results");
			// impMapRGB2.updateAndDraw();
			rt1.show("Results");
		}
		// MyLog.waitHere();

		// int levels;
		//
		// String lev = null;
		// if (twelve) {
		// lev = "12_livelli";
		// levels = 12;
		// } else {
		// lev = "5_livelli";
		// levels = 5;

		// Path path100 = Paths.get(dir10);
		// Path path101 = path100.getParent();
		//// ImagePlus scala = ImageUtils.generaScalaColori(num1);
		// String aux2 = path101 + "\\simul_" + lev + "\\" + myName +
		// "scala";
		// // MyLog.waitHere("aux1= " + aux1);
		// new FileSaver(scala).saveAsTiff(aux2);
		// UtilAyv.cleanUp2();
		// }
		long time4 = System.nanoTime();
		String tempo2 = MyTimeUtils.stringNanoTime(time4 - time3);
		IJ.log("Tempo totale  hh:mm:ss.ms " + tempo2);

		MyLog.waitHere("FINE");

	} // chiude

	/**
	 * Analisi di un profilo NON mediato
	 * 
	 * @param imp1
	 *            Immagine da analizzare
	 * @param ax
	 *            Coordinata x inizio segmento
	 * @param ay
	 *            Coordinata y inizio segmento
	 * @param bx
	 *            Coordinata x fine segmento
	 * @param by
	 *            Coordinata x fine segmento
	 * 
	 * @return outFwhm[0]=FWHM, outFwhm[1]=peak position
	 */

	private static double[] getProfile(ImagePlus imp1, int ax, int ay, int bx, int by, double dimPixel, boolean step) {

		if (imp1 == null) {
			IJ.error("getProfile ricevuto immagine null");
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
