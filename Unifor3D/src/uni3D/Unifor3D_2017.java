package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.File;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
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
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import utils.ArrayUtils;
import utils.ButtonMessages;
import utils.ImageUtils;
import utils.AboutBox;
import utils.MyCircleDetector;
import utils.MyConst;
import utils.MyGenericDialogGrid;
import utils.MyLine;
import utils.MyLog;
import utils.MyPlot;
import utils.MySphere;
import utils.MyStackUtils;
import utils.MyVersionUtils;
import utils.ReadDicom;
import utils.UtilAyv;

//=====================================================
//     Programma per uniformita' 3D per immagini COMBINED circolari
//     11 agosto 2016 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class Unifor3D_2017 implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = true;

	public void run(String arg) {
		boolean demo0 = false;

		// new MyAboutBox().about10("Unifor3D");

		new AboutBox().about("Unifor3D_2017", MyVersion.CURRENT_VERSION);
		IJ.wait(2000);
		new AboutBox().close();

		// double maxFitError = +20;
		// double maxBubbleGapLimit = 2;
		ArrayList<Integer> pixListSignal11 = new ArrayList<Integer>();
		ArrayList<Float> pixListDifference11 = new ArrayList<Float>();

		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}

		IJ.log("----------IW2AYV----------");
		UtilAyv.logResizer(500, 500, 100, 400);

		String dir10 = Prefs.get("prefer.Unifor3D_dir1", "none");
		dir10 = UtilAyv.dirSeparator(dir10);
		OpenDialog.setDefaultDirectory(dir10);
		OpenDialog od1 = new OpenDialog("SELEZIONARE LO STACK COMBINED PRIMA ACQUISIZIONE");
		String dir1 = od1.getPath();
		if (dir1 == null)
			return;
		Prefs.set("prefer.Unifor3D_dir1", dir1);

		String dir20 = Prefs.get("prefer.Unifor3D_dir2", "none");
		dir20 = UtilAyv.dirSeparator(dir20);
		OpenDialog.setDefaultDirectory(dir20);
		OpenDialog od2 = new OpenDialog("SELEZIONARE LO STACK COMBINED SECONDA ACQUISIZIONE");
		String dir2 = od2.getPath();
		if (dir2 == null)
			return;
		Prefs.set("prefer.Unifor3D_dir2", dir2);

		ImagePlus impCombined1 = UtilAyv.openImageNoDisplay(dir1, true);
		impCombined1.show();
		double dimPixel = ReadDicom.readDouble(
				ReadDicom.readSubstring(ReadDicom.readDicomParameter(impCombined1, MyConst.DICOM_PIXEL_SPACING), 1));

		// =================================================================
		// CLASSI ELEMENTARI
		// =================================================================
		int gridWidth = 2;
		int livello = 5;
		int gridHeight = livello;
		int gridSize = gridWidth * gridHeight;
		boolean buco = false;

		TextField[] tf2 = new TextField[gridSize];

		String[] lab2 = { "min% classe 1 (20)", "max% classe 1 (100)", "min% classe 2 (10)", "max% classe 2 (20)",
				"min% classe 3 (-10)", "max% classe 3 (10) ", "min% classe 4 (-20)", "max% classe 4 (-10)",
				"min% classe 5 (-90)", "max% classe 5 (-20)" };

		double[] value2 = new double[gridSize];
		double[] value3 = { 20, 100, 10, 20, -10, 10, -20, -10, -90, -20 };

		MyGenericDialogGrid mgdg = new MyGenericDialogGrid();

		for (int i1 = 0; i1 < value2.length; i1++) {
			value2[i1] = mgdg.getValue2(Prefs.get("prefer.Unifor3D_classi_" + i1, "0"));
		}

		int decimals = 0;
		String title2 = "LIMITI CLASSI PIXELS";

		if (mgdg.showDialog3(gridWidth, gridHeight, tf2, lab2, value2, value3, title2, decimals)) {
			// comodo il preset ????
			// displayValues2(gridSize, value2);
		}

		for (int i1 = 0; i1 < value2.length; i1++) {
			Prefs.set("prefer.Unifor3D_classi_" + i1, value2[i1]);
		}

		int[] minimi = new int[livello];
		int[] massimi = new int[livello];
		int conta = 0;
		for (int i1 = 0; i1 < livello; i1++) {
			minimi[i1] = (int) value2[conta++];
			massimi[i1] = (int) value2[conta++];
		}

		for (int i1 = 0; i1 < livello - 1; i1++) {
			if (minimi[i1] != massimi[i1 + 1])
				buco = true;
		}
		String[] myLabels = new String[livello];
		String sigmin = "";
		String sigmax = "";
		for (int i1 = 0; i1 < livello; i1++) {
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

		if (buco)
			MyLog.waitHere("LO SAI CHE LE CLASSI IMPOSTATE HANNO UN BUCOBUCONE?");

		// =================================================================
		// Utilizzo di ORTHOGONAL VIEWS per ricostruire le proiezioni nelle due
		// direzioni mancanti.
		// =================================================================

		double[] sphereA = MySphere.centerSphere(impCombined1, demo0);

		ImagePlus impCombined2 = UtilAyv.openImageNoDisplay(dir2, true);

		impCombined1.show();
		IJ.run(impCombined1, "Orthogonal Views", "");

		Orthogonal_Views ort1 = Orthogonal_Views.getInstance();

		// grazie alle coordinate centro sfera gia' calcolate da
		// MySphere.centerSphere posso impostare OrtogonalViews in modo che mi
		// dia le reali slice centrali delle tre direzioni. In questo modo mi
		// restano a disposizione per un eventuale calcolo in modalita' 2D
		// effettuato sulle immagini centrali della sfera.

		int crossx = (int) sphereA[0];
		int crossy = (int) sphereA[1];
		int crossz = (int) sphereA[2];
		ort1.setCrossLoc(crossx, crossy, crossz);
		IJ.wait(100);
		ImagePlus imp102 = ort1.getXZImage();
		ImagePlus impXZ1 = new Duplicator().run(imp102);
		IJ.wait(100);
		ImagePlus imp103 = ort1.getYZImage();
		ImagePlus impYZ1 = new Duplicator().run(imp103);
		IJ.wait(100);
		ImagePlus impXY1 = MyStackUtils.imageFromStack(impCombined1, crossz);
		impXY1.setTitle("XY1");

		Orthogonal_Views.stop();

		Overlay overXZ = new Overlay();
		impXZ1.setOverlay(overXZ);
		impXZ1.show();

		Overlay overYZ = new Overlay();
		impYZ1.setOverlay(overYZ);
		impYZ1.show();

		impCombined2.show();
		IJ.run(impCombined2, "Orthogonal Views", "");
		Orthogonal_Views ort2 = Orthogonal_Views.getInstance();
		ort1.setCrossLoc(crossx, crossy, crossz);
		IJ.wait(100);
		ImagePlus imp1022 = ort2.getXZImage();
		ImagePlus impXZ2 = new Duplicator().run(imp1022);
		IJ.wait(100);
		ImageUtils.closeImageWindow(impXZ2);
		ImagePlus imp1032 = ort2.getYZImage();
		ImagePlus impYZ2 = new Duplicator().run(imp1032);
		IJ.wait(100);
		ImagePlus impXY2 = MyStackUtils.imageFromStack(impCombined2, crossz);
		impXY2.setTitle("XY2");
		Orthogonal_Views.stop();
		ImageUtils.closeImageWindow(impXY2);

		// ImagePlus imp20 = MyStackUtils.imagesToStack16(sortedList2);

		// ===============================
		// PUNTO TRE : CALCOLO DEL VOLUME DIFFERENZA
		// ===============================

		ImagePlus impDiff = MyStackUtils.stackDiff(impCombined1, impCombined2);
		impDiff.setTitle("IMMAGINE DIFFERENZA");

		// ImagePlus stackDiff = stackDiffCalculation(imp10, imp20);
		impDiff.show();

		double diamMROI = sphereA[3] * MyConst.P3_AREA_PERC_80_DIAM;
		//
		// decido quali saranno le slice di start ed end per i calcoli di
		// uniformita'eccetera utilizzo la coordinata Z del centro sfera
		// (sphereA(2)) a cui tolgo e aggiungo il diametroMROI/2 il 3 aggiunto
		// alla endslice è trovato sperimentalmente (forse compensa
		// arrotondamenti & troncamenti vari, BOH?)

		int startSlice = (int) sphereA[2] - (int) (diamMROI / 2);
		int endSlice = startSlice + (int) diamMROI + 1;

		ImagePlus imp00 = MyStackUtils.imageFromStack(impCombined1, (int) sphereA[3]);
		double centerPos = ReadDicom.readDouble(
				ReadDicom.readSubstring(ReadDicom.readDicomParameter(imp00, MyConst.DICOM_IMAGE_POSITION), 1));
		//
		// int z1 = (int) sphereA[2];
		double radius = sphereA[3] / 2;
		double radius3 = diamMROI / 2;
		// int count = -1;
		ImagePlus impSliceCombined1;
		ImagePlus impSliceCombined2;

		// ==========================================================================
		// CALCOLI RIPETUTO SUI VARI STRATI
		// ==========================================================================

		for (int zslice = startSlice - 1; zslice < endSlice + 1; zslice++) {
			IJ.showStatus("" + zslice + " / " + endSlice);

			// ===============================================
			double distanceFromCenter = sphereA[2] - zslice;

			int diamEXT2 = (int) Math.sqrt(radius * radius - distanceFromCenter * distanceFromCenter) * 2;
			double radius1 = diamEXT2 / 2;

			double diamMROI2 = Math.sqrt(radius3 * radius3 - distanceFromCenter * distanceFromCenter) * 2;
			if (UtilAyv.isNaN(diamMROI2))
				diamMROI2 = 0;

			double radius2 = diamMROI2 / 2;
			// ==== coloro le SLICES man mano le elaboro ====
			double plotPos = zslice * dimPixel;
			Color verde = new Color(00, 255, 0, 80); // green
			Color rosa = new Color(255, 50, 255, 80); // purple

			// ==
			double start = 0;
			double stop = 0;

			start = sphereA[0] - radius1;
			stop = sphereA[0] - radius2;
			impXZ1.setRoi(new Line(start, plotPos, stop, plotPos));
			impXZ1.getRoi().setStrokeColor(verde);
			impXZ1.getRoi().setStrokeWidth(1.5);
			overXZ.addElement(impXZ1.getRoi());

			impXZ1.setRoi(new Line(sphereA[0] - radius2, plotPos, sphereA[0] + radius2, plotPos));
			impXZ1.getRoi().setStrokeColor(rosa);
			overXZ.addElement(impXZ1.getRoi());

			start = sphereA[0] + radius2;
			stop = sphereA[0] + radius1;
			impXZ1.setRoi(new Line(start, plotPos, stop, plotPos));
			impXZ1.getRoi().setStrokeColor(verde);
			overXZ.addElement(impXZ1.getRoi());
			// ===================
			start = sphereA[1] - radius1;
			stop = sphereA[1] - radius2;
			impYZ1.setRoi(new Line(plotPos, start, plotPos, stop));
			impYZ1.getRoi().setStrokeColor(verde);
			overYZ.addElement(impYZ1.getRoi());
			// --
			impYZ1.setRoi(new Line(plotPos, sphereA[1] - radius2, plotPos, sphereA[1] + radius2));
			impYZ1.getRoi().setStrokeColor(rosa);
			overYZ.addElement(impYZ1.getRoi());
			// --
			start = sphereA[1] + radius2;
			stop = sphereA[1] + radius1;
			impYZ1.setRoi(new Line(plotPos, start, plotPos, stop));
			impYZ1.getRoi().setStrokeColor(verde);
			overYZ.addElement(impYZ1.getRoi());
			// IJ.log("zslice= " + zslice + " distanceFromCenter= " +
			// distanceFromCenter + " diamEXT2= " + diamEXT2
			// + " diamMROI2= " + diamMROI2);

			impSliceCombined1 = MyStackUtils.imageFromStack(impCombined1, zslice);
			impSliceCombined2 = MyStackUtils.imageFromStack(impCombined2, zslice);

			// ==================================================================================
			// ACCODO I PIXEL DI SEGNALE DELLA ROI AL VETTORE DEI PIXEL DELLO
			// STACK SEGNALE
			// ==================================================================================

			pixVectorize1(impSliceCombined1, sphereA[0], sphereA[1], diamMROI2, pixListSignal11);

			// ==================================================================================
			// ACCODO I PIXEL DI SEGNALE DELLA ROI AL VETTORE DEI PIXEL DELLO
			// STACK DIFFERENZA
			// ==================================================================================

			ImagePlus impSliceDiff = MyStackUtils.imageFromStack(impDiff, zslice);
			pixVectorize2(impSliceDiff, sphereA[0], sphereA[1], diamMROI2, pixListDifference11);
			impSliceCombined1
					.setRoi(new OvalRoi(sphereA[0] - diamMROI2 / 2, sphereA[1] - diamMROI2 / 2, diamMROI2, diamMROI2));

			impSliceCombined1.close();
			impSliceCombined2.close();
			impSliceDiff.close();
		}

		impXZ1.deleteRoi();
		impYZ1.deleteRoi();

		int[] pixListSignal = ArrayUtils.arrayListToArrayInt(pixListSignal11);
		double meanMROI = ArrayUtils.vetMean(pixListSignal);

		float[] pixListDifference = ArrayUtils.arrayListToArrayFloat(pixListDifference11);
		double sd_diff = ArrayUtils.vetSdKnuth(pixListDifference);
		double snrMROI = meanMROI * Math.sqrt(2) / sd_diff;

		int minSignal = ArrayUtils.vetMin(pixListSignal);
		int maxSignal = ArrayUtils.vetMax(pixListSignal);

		// MyLog.waitHere("Max= " + maxSignal + " Min= " + minSignal);
		double uiPerc1 = uiPercCalculation(maxSignal, minSignal);
		double uiNew = naadCalculation(pixListSignal);

		int countS = -1;
		int[][] matClassi = new int[6][2];
		int[] myColor = new int[livello];

		myColor[0] = ((255 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
		myColor[1] = ((255 & 0xff) << 16) | ((165 & 0xff) << 8) | (0 & 0xff);
		myColor[2] = ((255 & 0xff) << 16) | ((255 & 0xff) << 8) | (0 & 0xff);
		myColor[3] = ((124 & 0xff) << 16) | ((252 & 0xff) << 8) | (50 & 0xff);
		myColor[4] = ((0 & 0xff) << 16) | ((128 & 0xff) << 8) | (0 & 0xff);

		ImageStack stackSimulata = new ImageStack(impCombined1.getWidth(), impCombined1.getHeight());
		ImagePlus impSimulata = null;
		ImageProcessor ipSimulata = null;
		double thisPosS;
		double projectS;
		double radius1S;
		double diamEXT2S;
		double radius2S;
		double diamMROI2S;
		String sliceInfo1;
		String sliceInfo2;
		ImageWindow iwSimulata;

		// =============================================
		/// IMMAGINI SIMULATE SECONDO SANTA ROMANA NEMA
		// =============================================

		for (int i1 = 0; i1 < impCombined1.getNSlices(); i1++) {
			IJ.showStatus("" + i1 + " / " + endSlice);

			// ===============================================
			impSliceCombined1 = MyStackUtils.imageFromStack(impCombined1, i1 + 1);
			thisPosS = ReadDicom.readDouble(ReadDicom
					.readSubstring(ReadDicom.readDicomParameter(impSliceCombined1, MyConst.DICOM_IMAGE_POSITION), 1));

			countS++;

			// Overlay over111S = new Overlay();
			// impSliceCombined1.setOverlay(over111S);

			// ====================================================================
			// CALCOLO CON PITAGORA (QUELLO DE "IL TEOREMA") IL DIAMETRO
			// ESTERNO
			// E DELLA MROI PER LO STRATO ATTUALE
			// ====================================================================

			projectS = Math.abs(centerPos - thisPosS);
			radius1S = sphereA[3] / 2;
			diamEXT2S = Math.sqrt(radius1S * radius1S - projectS * projectS) * 2;
			if (UtilAyv.isNaN(diamEXT2S))
				diamEXT2S = 0;

			radius2S = diamMROI / 2;
			diamMROI2S = Math.sqrt(radius2S * radius2S - projectS * projectS) * 2;
			if (UtilAyv.isNaN(diamMROI2S))
				diamMROI2S = 0;

			// demo0, test);
			impSimulata = ImageUtils.generaSimulataMultiColori(meanMROI, impSliceCombined1, minimi, massimi, myColor);

			// impSimulata.show();
			ipSimulata = impSimulata.getProcessor();
			if (countS == 0)
				stackSimulata.update(ipSimulata);
			sliceInfo1 = impSimulata.getTitle();
			sliceInfo2 = (String) impSimulata.getProperty("Info");
			// aggiungo i dati header alle singole immagini dello stack
			if (sliceInfo2 != null)
				sliceInfo1 = sliceInfo1 + "\n" + sliceInfo2;
			stackSimulata.addSlice(sliceInfo2, ipSimulata);

			iwSimulata = impSimulata.getWindow();
			if (iwSimulata != null)
				iwSimulata.dispose();

			impSimulata.close();
			impSliceCombined1.close();
		}

		ImagePlus simulataStack = new ImagePlus("STACK_IMMAGINI_SIMULATE", stackSimulata);
		simulataStack.show();

		// qui devo realizzare il conteggio pixel classi

		matClassi = numeroPixelsColori(simulataStack, myColor);

		double totColorati = 0;
		for (int i1 = 0; i1 < matClassi.length - 1; i1++) {
			totColorati = totColorati + matClassi[i1][1];
		}
		double totFondo = matClassi[matClassi.length - 1][1];

		double[] matPercClassi = new double[matClassi.length - 1];
		for (int i1 = 0; i1 < matClassi.length - 1; i1++) {
			matPercClassi[i1] = matClassi[i1][1] * 100 / (totColorati + totFondo);
		}

		ImagePlus impColors = ImageUtils.generaScalaColori(myColor, myLabels);
		impColors.show();

		// IJ.log("NORMAL VECTOR mean11 pixels SEGNALE= " + mean11 + " devst11
		// pixels DIFFERENZA= " + devst11);

		// creo un imageProcessor col contenuto del vettore SEGNALE
		int aaa = pixListSignal.length;
		double www11 = Math.sqrt((double) aaa);
		int www = (int) www11 + 1;
		short[] pixList2 = new short[www * www];
		for (int i1 = 0; i1 < aaa; i1++) {
			pixList2[i1] = (short) pixListSignal[i1];
		}
		double mean22 = ArrayUtils.vetMean(pixList2);
		double devst22 = ArrayUtils.vetSdKnuth(pixList2);
		double snr22 = (mean22 * Math.sqrt(2.0)) / devst22;
		double snr3D = (meanMROI * Math.sqrt(2.0)) / sd_diff;

		ImageProcessor ipx = new ShortProcessor(www, www, pixList2, null);
		ImagePlus impx = new ImagePlus("SEGNALE", ipx);
		IJ.run(impx, "Histogram", "");

		// creo un imageProcessor col contenuto del vettore DIFFERENZA
		int bbb = pixListDifference.length;
		double zzz11 = Math.sqrt((double) bbb);
		int zzz = (int) zzz11 + 1;
		float[] pixList3 = new float[zzz * zzz];
		for (int i1 = 0; i1 < bbb; i1++) {
			pixList3[i1] = (float) pixListDifference[i1];
		}

		ImageProcessor ipz = new FloatProcessor(zzz, zzz, pixList3, null);
		ImagePlus impz = new ImagePlus("DIFFERENZA", ipz);
		// impz.show();
		IJ.run(impz, "Histogram", "bins=256 use x_min=-96 x_max=136 y_max=Auto");
		// IJ.run(impz, "Histogram", "");
		// MyLog.waitHere();

		ResultsTable rt1 = ResultsTable.getResultsTable();
		rt1.incrementCounter();
		rt1.addValue("TIPO ", "ANALISI 3D VOLUME SFERA");

		rt1.addValue("Fantoccio [x:y:z:d] ", IJ.d2s(sphereA[0], 0) + ":" + IJ.d2s(sphereA[1], 0) + ":"
				+ IJ.d2s(sphereA[2], 0) + ":" + IJ.d2s(sphereA[3], 0));
		rt1.addValue("SEGNALE", meanMROI);
		rt1.addValue("RUMORE", sd_diff);
		rt1.addValue("SNR", snrMROI);
		rt1.addValue("MAX", maxSignal);
		rt1.addValue("MIN", minSignal);
		rt1.addValue("UI%", uiPerc1);
		rt1.addValue("NAAD", uiNew);
		rt1.addValue("Voxels colorati", totColorati);
		rt1.addValue("Voxels fondo", totFondo);
		for (int i2 = 0; i2 < minimi.length; i2++) {
			rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2], matClassi[i2][1]);
		}

		rt1.addValue("Voxels colorati [%]", ResultsTable.d2s(totColorati * 100 / (totColorati + totFondo), 2));
		rt1.addValue("Voxels fondo [%]",
				ResultsTable.d2s(matClassi[matClassi.length - 1][1] * 100 / (totColorati + totFondo), 2));
		for (int i2 = 0; i2 < minimi.length; i2++) {
			rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2] + "[%]", ResultsTable.d2s(matPercClassi[i2], 2));
		}

		// ===========================================================
		// CALCOLO UNIFORMITA' 2D ( E TE PAREVA CHE CE LA FACEVAMO MANCARE )
		// ===========================================================

		int direction = 1;
		double maxFitError = 20.0;
		double maxBubbleGapLimit = 2.0;
		boolean demo1 = false;
		String[] tit1 = { "ANALISI 2D PROIEZ.XY", "ANALISI 2D PROIEZ.YZ", "ANALISI 2D PROIEZ.XZ" };
		String[] tit2 = { "coloriSimulataXY", "coloriSimulataYZ", "coloriSimulataXZ" };
		int[][] matClassiDIR = new int[6][2];

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
			double[] circleDIR = MySphere.centerCircleCannyEdge(impDIR1, direction, maxFitError, maxBubbleGapLimit, demo1);
			circleDIR[2] = sphereA[3];

			Overlay overDIR = new Overlay();
			impDIR1.setOverlay(overDIR);
			impDIR1.setRoi(new OvalRoi(circleDIR[0] - circleDIR[2] / 2, circleDIR[1] - circleDIR[2] / 2, circleDIR[2],
					circleDIR[2]));
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
			double meanDIR = statDIR.mean;

			double uiPercDIR = uiPercCalculation(statDIR.max, statDIR.min);
			ImagePlus impDiffDIR = UtilAyv.genImaDifference(impDIR1, impDIR2);
			Overlay overDiffDIR = new Overlay();
			overDiffDIR.setStrokeColor(Color.red);
			impDiffDIR.setOverlay(overDiffDIR);
			impDiffDIR.setRoi(new OvalRoi(circleDIR[0] - diamMROIDIR / 2, circleDIR[1] - diamMROIDIR / 2, diamMROIDIR,
					diamMROIDIR));
			impDiffDIR.getRoi().setStrokeColor(Color.red);
			overDiffDIR.addElement(impDiffDIR.getRoi());
			ImageStatistics statImaDiffDIR = impDiffDIR.getStatistics();
			impDiffDIR.deleteRoi();
			double meanImaDiffDIR = statImaDiffDIR.mean;
			double sd_ImaDiffDIR = statImaDiffDIR.stdDev;
			double noiseImaDiffDIR = sd_ImaDiffDIR / Math.sqrt(2);
			double snRatioDIR = Math.sqrt(2) * meanDIR / sd_ImaDiffDIR;
			ArrayList<Integer> pixListSignalXY1 = new ArrayList<Integer>();
			pixVectorize1(impDIR1, circleDIR[0], circleDIR[1], diamMROIDIR, pixListSignalXY1);
			int[] pixListSignalDIR = ArrayUtils.arrayListToArrayInt(pixListSignalXY1);
			double naadDIR = naadCalculation(pixListSignalDIR);

			ImagePlus imaDIRsimulata = ImageUtils.generaSimulataMultiColori(meanDIR, impDIR1, minimi, massimi, myColor);
			imaDIRsimulata.setTitle(tit2[i1]);

			imaDIRsimulata.show();

			matClassiDIR = numeroPixelsColori(imaDIRsimulata, myColor);

			double totColoratiDIR = 0;
			for (int i2 = 0; i2 < matClassiDIR.length - 1; i2++) {
				totColoratiDIR = totColoratiDIR + matClassiDIR[i2][1];
			}
			double totFondoDIR = matClassiDIR[matClassiDIR.length - 1][1];

			double[] matPercClassiDIR = new double[matClassiDIR.length - 1];
			for (int i2 = 0; i2 < matClassiDIR.length - 1; i2++) {
				matPercClassiDIR[i2] = matClassiDIR[i2][1] * 100 / (totColoratiDIR + totFondoDIR);
			}

			// boolean verbose = false;
			// boolean test = false;
			// int[][] classiSimulata = generaSimulata((int) (circleDIR[0] -
			// diamMROIDIR / 2),
			// (int) (circleDIR[1] - diamMROIDIR / 2), diamMROIDIR, impDIR1,
			// "ASSIALE", step, verbose, test);

			rt1.incrementCounter();
			rt1.addValue("TIPO ", tit1[i1]);
			rt1.addValue("Fantoccio [x:y:z:d] ", IJ.d2s(circleDIR[0], 0) + ":" + IJ.d2s(circleDIR[1], 0) + ":" + "___"
					+ ":" + IJ.d2s(circleDIR[2], 0));

			rt1.addValue("SEGNALE", meanDIR);
			rt1.addValue("RUMORE", noiseImaDiffDIR);
			rt1.addValue("SNR", snRatioDIR);
			rt1.addValue("MAX", statDIR.max);
			rt1.addValue("MIN", statDIR.min);
			rt1.addValue("UI%", uiPercDIR);
			rt1.addValue("NAAD", naadDIR);

			rt1.addValue("Voxels colorati", totColoratiDIR);
			rt1.addValue("Voxels fondo", totFondoDIR);
			for (int i2 = 0; i2 < minimi.length; i2++) {
				rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2], matClassiDIR[i2][1]);
			}

			rt1.addValue("Voxels colorati [%]",
					ResultsTable.d2s(totColoratiDIR * 100 / (totColoratiDIR + totFondoDIR), 2));
			rt1.addValue("Voxels fondo [%]",
					ResultsTable.d2s(matClassi[matClassiDIR.length - 1][1] * 100 / (totColoratiDIR + totFondoDIR), 2));
			for (int i2 = 0; i2 < minimi.length; i2++) {
				rt1.addValue("classe >" + minimi[i2] + "<" + massimi[i2] + "[%]",
						ResultsTable.d2s(matPercClassiDIR[i2], 2));
			}
		}
		rt1.show("Result");

		IJ.log("***********************************************************"
				+ "\n**__CALCOLI ESEGUITI SOLO SUI PIXEL__**" + "\n**______APPARTENENTI ALLE ROI_______**"
				+ "\n**__(INSERITI IN UN UNICO VETTORE) ____**"
				+ "\n***********************************************************");
		IJ.log("mean pixels SEGNALE= " + IJ.d2s(meanMROI, 3));
		IJ.log("unint% pixels SEGNALE= " + IJ.d2s(uiPerc1, 3));
		IJ.log("uiNEW pixels SEGNALE= " + IJ.d2s(uiNew, 3));
		IJ.log("devSt pixels DIFFERENZA= " + IJ.d2s(sd_diff, 3) + " %");
		IJ.log("SNR_3D= " + IJ.d2s(snr3D, 3));
		IJ.log("-----------------------------");
		IJ.log("SUDDIVISIONE IN CLASSI STACK IMMAGINI SIMULATE");
		for (int i2 = 0; i2 < minimi.length; i2++) {
			IJ.log("classe >" + minimi[i2] + "<" + massimi[i2] + " = " + matClassi[i2][1]);
		}
		IJ.log("classe <" + minimi[minimi.length - 1] + " = " + IJ.d2s(matClassi[5][1], 3));

		IJ.log("SUDDIVISIONE IN CLASSI PERCENTUALI STACK IMMAGINI SIMULATE (escludendo il fondo)");
		IJ.log("pixel colorati= " + totColorati);
		for (int i2 = 0; i2 < minimi.length; i2++) {
			IJ.log("classe >" + minimi[i2] + "<" + massimi[i2] + " = " + IJ.d2s(matPercClassi[i2], 3) + " %");
		}

		IJ.log("-----------------------------");

		// ResultsTable rt2 = vectorResultsTable(classi);

		// rt2.show("Results");

	} // chiude
		// run

	/**
	 * 13/11/2016 Nuovo algoritmo per uniformita' per immagini con grappa datomi
	 * da Lorella CHIAMASI NAAD
	 * 
	 * @param pixListSignal
	 * @return
	 */

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

	// ############################################################################

	/**
	 * Calcolo della distanza tra un punto ed una circonferenza
	 * 
	 * @param x1
	 *            coord. x punto
	 * @param y1
	 *            coord. y punto
	 * @param x2
	 *            coord. x centro
	 * @param y2
	 *            coord. y centro
	 * @param r2
	 *            raggio
	 * @return distanza
	 */
	public static double pointCirconferenceDistance(int x1, int y1, int x2, int y2, int r2) {

		double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) - r2;
		return dist;
	}

	public static int[][] numeroPixelsColori(ImagePlus imp1, int[] myColor) {

		if (imp1 == null) {
			IJ.error("numeroPixelClassi ricevuto null");
			return (null);
		}
		int width = imp1.getWidth();
		int height = imp1.getHeight();
		int offset = 0;
		int[][] vetClassi = new int[myColor.length + 1][2];
		boolean manca = true;
		for (int i1 = 0; i1 < myColor.length; i1++) {
			vetClassi[i1][0] = myColor[i1];
		}
		if (imp1.getImageStackSize() > 1) {
			for (int z1 = 0; z1 < imp1.getImageStackSize(); z1++) {
				ImagePlus imp2 = MyStackUtils.imageFromStack(imp1, z1 + 1);
				if (imp2 == null)
					continue;
				ImageProcessor ip2 = imp2.getProcessor();
				int[] pixels2 = (int[]) ip2.getPixels();
				int pix2 = 0;
				for (int y1 = 0; y1 < height; y1++) {
					for (int x1 = 0; x1 < width; x1++) {
						offset = y1 * width + x1;
						pix2 = pixels2[offset];
						manca = true;
						for (int i1 = 0; i1 < myColor.length; i1++)
							if (pix2 == vetClassi[i1][0]) {
								vetClassi[i1][1] = vetClassi[i1][1] + 1;
								manca = false;
								break;
							}
						if (manca) {
							vetClassi[5][1] = vetClassi[5][1] + 1;
							manca = false;
						}
					}
				}
			}
		} else {
			ImageProcessor ip1 = imp1.getProcessor();
			int[] pixels1 = (int[]) ip1.getPixels();
			int pix1 = 0;
			for (int y1 = 0; y1 < height; y1++) {
				for (int x1 = 0; x1 < width; x1++) {
					offset = y1 * width + x1;
					pix1 = pixels1[offset];
					manca = true;
					for (int i1 = 0; i1 < myColor.length; i1++)
						if (pix1 == vetClassi[i1][0]) {
							vetClassi[i1][1] = vetClassi[i1][1] + 1;
							manca = false;
							break;
						}
					if (manca) {
						vetClassi[5][1] = vetClassi[5][1] + 1;
						manca = false;
					}
				}
			}
		}
		return (vetClassi);

	} // classi

} // ultima
