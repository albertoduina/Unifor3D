package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import utils.ArrayUtils;
import utils.ImageUtils;
import utils.AboutBox;
import utils.MyCircleDetector;
import utils.MyConst;
import utils.MyLine;
import utils.MyLog;
import utils.MyPlot;
import utils.MyStackUtils;
import utils.MyVersionUtils;
import utils.ReadDicom;
import utils.TableSequence;
import utils.TableUtils;
import utils.UtilAyv;

//=====================================================
//     Programma per plot 3D per immagini ucombined circolari
//     11 agosto 2016 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class Uncombined3D_ implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = true;

	public void run(String arg) {

		// new MyAboutBox().about10("Unifor3D");
		new AboutBox().about("Uncombined3D", MyVersionUtils.CURRENT_VERSION);
		IJ.wait(2000);
		new AboutBox().close();

		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}

		// chiede di selezionare manualmente le cartelle con le
		// immagini Si suppone che le immagini siano trasferite utilizzando un
		// hard-disk OPPURE che venga fatto un DicomSorter.
		// QUINDI: IL PROGRAMMA SI FIDA (sicuramente sbagliando) DELL'OPERATORE

		// ===============================
		// PUNTO UNO : APRIRE STACK
		// ===============================

		String dir10 = Prefs.get("prefer.Uncombined3D_dir1", "none");
		DirectoryChooser.setDefaultDirectory(dir10);
		DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA IMMAGINI");
		String dir1 = od1.getDirectory();
		Prefs.set("prefer.Uncombine3D_dir1", dir1);
		DirectoryChooser.setDefaultDirectory(dir10);
		// ------------------------------
		// ------------------------------
		String[] dir1a = new File(dir1).list();
		String[] dir1b = new String[dir1a.length];
		for (int i1 = 0; i1 < dir1a.length; i1++) {
			dir1b[i1] = dir1 + "\\" + dir1a[i1];
		}
		String[][] sortedList1 = pathSorterUncombined(dir1b);

		String[][] vetConta = contaList(sortedList1);

		MyLog.waitHere();

	} // chiude

	public static String[][] contaList(String[][] mat1) {
		List<Integer> list1 = new ArrayList<Integer>();
		List<String> list2 = new ArrayList<String>();
		String old = "";
		int conta = 0;
		MyLog.waitHere("mat1.length= "+mat1.length);
		MyLog.waitHere("mat1[0].length= "+mat1[0].length);
		MyLog.waitHere();
		for (int i1 = 0; i1 < mat1[0].length; i1++) {
			String aux1 = mat1[1][i1];
			if (i1 == 0)
				old = aux1;
			if (old.equals(aux1)) {
				conta++;
			} else {
				list1.add(conta);
				list2.add(aux1);
				conta = 1;
				old = aux1;
			}
		}
		String[] vetCoil = ArrayUtils.arrayListToArrayString(list2);
		int[] vetConta = ArrayUtils.arrayListToArrayInt(list1);
		String[][] vetOut = new String[2][vetCoil.length];

		MyLog.logVector(vetCoil, "vetCoil");
		MyLog.waitHere();
		MyLog.logVector(vetConta, "vetConta");
		MyLog.waitHere();
		for (int i1 = 0; i1 < vetCoil.length; i1++) {
			vetOut[0][i1] = vetCoil[i1];
			vetOut[1][i1] = "" + vetConta[i1];
		}
		MyLog.logMatrixDimensions(vetOut, "vetOut");

		return vetOut;
	}

	// run

	public static ImagePlus stackDiffCalculation(ImagePlus stack1, ImagePlus stack2) {
		ImageStack newStack = new ImageStack(stack1.getWidth(), stack1.getHeight());
		ImagePlus imp1;
		ImagePlus imp2;
		for (int i1 = 1; i1 < stack1.getImageStackSize(); i1++) {
			imp1 = MyStackUtils.imageFromStack(stack1, i1);
			imp2 = MyStackUtils.imageFromStack(stack2, i1);

			ImagePlus impDiff = UtilAyv.diffIma(imp1, imp2);
			ImageProcessor ipDiff = impDiff.getProcessor();
			if (i1 == 1)
				newStack.update(ipDiff);
			String sliceInfo1 = imp1.getTitle();
			String sliceInfo2 = (String) imp1.getProperty("Info");
			if (sliceInfo2 != null)
				sliceInfo1 += "\n" + sliceInfo2;
			newStack.addSlice(sliceInfo2, ipDiff);
		}
		ImagePlus newImpStack = new ImagePlus("STACK_IMMAGINI_DIFFERENZA", newStack);

		return newImpStack;
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

	public static void pixVectorize(ImagePlus imp11, double xCenterMROI, double yCenterMROI, double diamMROI,
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

	public static int[] pixClassi(int[] vetPix) {

		int value;
		// per le prove utilizzo un vettore da 0 a 4095
		int[] vetClassi = new int[4096];
		for (int i1 = 0; i1 < vetPix.length; i1++) {
			value = vetPix[i1];
			vetClassi[value]++;
		}
		return vetClassi;
	}

	public static int[] getMinMax(int[] a) {
		int min = Integer.MAX_VALUE;
		int max = -Integer.MAX_VALUE;
		int value;
		for (int i = 0; i < a.length; i++) {
			value = a[i];
			if (value < min)
				min = value;
			if (value > max)
				max = value;
		}
		int[] minAndMax = new int[2];
		minAndMax[0] = min;
		minAndMax[1] = max;
		return minAndMax;
	}

	// ############################################################################

	/***
	 * sort del vettore path in base a BOBINA e posizione immagine
	 * 
	 * @param path
	 * @return
	 */
	public static String[][] pathSorterUncombined(String[] path) {
		IJ.showStatus("LOAD");
		ArrayList<String> list1 = new ArrayList<String>();
		ArrayList<String> list2 = new ArrayList<String>();
		ArrayList<String> list3 = new ArrayList<String>();

		if ((path == null) || (path.length == 0)) {
			IJ.log("pathSorter: path problems");
			return null;
		}
		Opener opener1 = new Opener();
		// test disponibilita' files
		for (int w1 = 0; w1 < path.length; w1++) {
			IJ.showStatus("SPETA EN MOMENT");
			IJ.showProgress(w1, path.length);
			// IJ.showStatus("GenerateSequenceTable= " + w1 + " / " +
			// path.length);
			if (opener1.getFileType(path[w1]) == Opener.DICOM) {
				ImagePlus imp1 = opener1.openImage(path[w1]);
				if (imp1 != null) {
					list1.add(path[w1]);
					list2.add(ReadDicom.readDicomParameter(imp1, MyConst.DICOM_COIL));
					list3.add(ReadDicom.readDicomParameter(imp1, MyConst.DICOM_SLICE_LOCATION));
				}
				imp1.close();
				// System.gc();
			}
		}

		String[] path1 = ArrayUtils.arrayListToArrayString(list1);
		String[] sliceCoil = ArrayUtils.arrayListToArrayString(list2);
		String[] slicePosition = ArrayUtils.arrayListToArrayString(list3);
		// ResultsTable rt3 = vectorResultsTable2(sliceCoil);
		// rt3.show("Log sliceCoil");
		String[][] matStr0 = new String[3][path1.length];
		for (int w1 = 0; w1 < path1.length; w1++) {
			matStr0[0][w1] = path1[w1];
			matStr0[1][w1] = "" + sliceCoil[w1];
			matStr0[2][w1] = "" + slicePosition[w1];
			// MyLog.waitHere("" + sliceCoil[w1] + " " + slicePosition[w1]);
		}
		ResultsTable rt3 = vectorResultsTable2(matStr0);
		rt3.show("INIZIALE");
		MyLog.logMatrixDimensions(matStr0, "matStr0");
		String[][] matStr1 = minsort2(matStr0, 2);
		MyLog.logMatrixDimensions(matStr1, "matStr1");
		ResultsTable rt4 = vectorResultsTable2(matStr1);
		rt4.show("PRIMO Sortato per POSITION");

		String[][] matStr2 = minsort2(matStr1, 1);
		MyLog.logMatrixDimensions(matStr2, "matStr0");

		ResultsTable rt5 = vectorResultsTable2(matStr2);
		rt5.show("SECONDO Sortato per COIL");

		return matStr2;
	}

	/***
	 * bubble sort in base alla posizione dell'immagine
	 * 
	 * @param path
	 * @param slicePosition
	 * @return
	 */
	public static String[] bubbleSortPath(String[] path, String[] slicePosition) {

		if (path == null)
			return null;
		if (slicePosition == null)
			return null;
		if (!(path.length == slicePosition.length))
			return null;
		if (path.length < 2) {
			return path;
		}
		String[] sortedPath = new String[path.length];
		sortedPath = path;
		for (int i1 = 0; i1 < path.length; i1++) {
			for (int i2 = 0; i2 < path.length - 1 - i1; i2++) {
				double pointer1 = ReadDicom.readDouble(slicePosition[i2]);
				double pointer2 = ReadDicom.readDouble(slicePosition[i2 + 1]);
				if (pointer1 > pointer2) {
					String positionSwap = slicePosition[i2];
					slicePosition[i2] = slicePosition[i2 + 1];
					slicePosition[i2 + 1] = positionSwap;
					String pathSwap = sortedPath[i2];
					sortedPath[i2] = sortedPath[i2 + 1];
					sortedPath[i2 + 1] = pathSwap;
				}
			}
		}
		return sortedPath;
	}

	public static String[][] minsort(String[][] tableIn, int key) {

		String[][] tableOut = duplicateTable(tableIn);
		double[] vetKey = new double[tableIn.length];
		int[] vetIndex = new int[tableIn.length];

		for (int i1 = 0; i1 < tableOut.length; i1++) {
			String strKey = getKey(tableOut, i1, key);
			if (strKey == null)
				strKey = "9999999999999999";
			if (UtilAyv.isNumeric(strKey))
				vetKey[i1] = Double.parseDouble(strKey);
			else
				vetKey[i1] = Double.NaN;
			vetIndex[i1] = i1;
		}

		// effettuo minsort su key, gli altri campi andranno in parallelo
		double aux1 = 0;
		int aux2 = 0;
		for (int i1 = 0; i1 < vetKey.length; i1++) {
			for (int i2 = i1 + 1; i2 < vetKey.length; i2++) {
				if (vetKey[i2] < vetKey[i1]) {
					aux1 = vetKey[i1];
					vetKey[i1] = vetKey[i2];
					vetKey[i2] = aux1;
					aux2 = vetIndex[i1];
					vetIndex[i1] = vetIndex[i2];
					vetIndex[i2] = aux2;
				}
			}
		}

		for (int i1 = 0; i1 < tableOut[0].length; i1++) {
			for (int i2 = 0; i2 < vetIndex.length; i2++) {
				tableOut[i2][i1] = tableIn[vetIndex[i2]][i1];
			}
		}
		return tableOut;
	}

	public static String[][] minsort2(String[][] tableIn, int key) {
		
		String[][] tableOut = duplicateTable(tableIn);
//		MyLog.logMatrix(tableOut, "tableOut duplicata");
//		IJ.log("tableIn.length= "+tableIn.length);
//		IJ.log("tableIn[0].length= "+tableIn[0].length);
//		MyLog.here();
		
		String[] vetKey = new String[tableIn[0].length];
		int[] vetIndex = new int[tableIn[0].length];

		for (int i1 = 0; i1 < tableOut[0].length; i1++) {
			String strKey = getKey(tableOut, i1, key);
			vetKey[i1] = strKey;
			vetIndex[i1] = i1;
		}
		// effettuo minsort su key, gli altri campi andranno in parallelo
		String aux1 = "zzzzz";
		int aux2 = 0;
		boolean numeric = true;

		for (int i1 = 0; i1 < vetKey.length; i1++) {
			try {
				Double.parseDouble(vetKey[i1]);
			} catch (NumberFormatException e) {
				numeric = false;
			}
		}

		for (int i1 = 0; i1 < vetKey.length; i1++) {
			for (int i2 = i1 + 1; i2 < vetKey.length; i2++) {
				if (numeric) {
					if (Double.parseDouble(vetKey[i2]) < Double.parseDouble(vetKey[i1])) {
						aux1 = vetKey[i1];
						vetKey[i1] = vetKey[i2];
						vetKey[i2] = aux1;
						aux2 = vetIndex[i1];
						vetIndex[i1] = vetIndex[i2];
						vetIndex[i2] = aux2;
					}
				} else {
					if (vetKey[i2].compareToIgnoreCase(vetKey[i1]) < 0) {
						aux1 = vetKey[i1];
						vetKey[i1] = vetKey[i2];
						vetKey[i2] = aux1;
						aux2 = vetIndex[i1];
						vetIndex[i1] = vetIndex[i2];
						vetIndex[i2] = aux2;
					}
				}
			}
		}

//		MyLog.logMatrixDimensions(tableOut, "tableOut duplicata");
//		MyLog.logVector(vetIndex, "vetIndex");

//		IJ.log("tableOut.length= "+tableOut.length);
//		IJ.log("tableOut[0].length= "+tableOut[0].length);
		
		for (int i1 = 0; i1 < tableOut.length; i1++) {
			for (int i2 = 0; i2 < tableOut[0].length; i2++) {
				// IJ.log("i1= "+i1+" i2= "+i2);
				// MyLog.waitHere();
				tableOut[i1][i2] = tableIn[i1][vetIndex[i2]];
			}
		}
		return tableOut;
	}

	public static String[][] duplicateTable(String[][] inTable) {
		if (inTable == null)
			return null;
		if (inTable.length == 0)
			return null;
		String[][] outTable = new String[inTable.length][inTable[0].length];
		for (int i1 = 0; i1 < inTable.length; i1++) {
			for (int i2 = 0; i2 < inTable[0].length; i2++) {
				outTable[i1][i2] = inTable[i1][i2];
			}
		}
		return outTable;
	}

	public static String getKey(String[][] strTabella, int riga, int key) {
		if (strTabella == null)
			return null;
		return strTabella[key][riga];
	}

	/***
	 * lettura delle posizioni delle immagini
	 * 
	 * @param listIn
	 * @return
	 */
	public static String[] listSlicePosition(String[] listIn) {
		String[] slicePosition = new String[listIn.length];
		for (int w1 = 0; w1 < listIn.length; w1++) {
			ImagePlus imp1 = UtilAyv.openImageNoDisplay(listIn[w1], true);
			String slicePosition1 = ReadDicom.readDicomParameter(imp1, MyConst.DICOM_SLICE_LOCATION);
			slicePosition[w1] = slicePosition1;
		}
		return slicePosition;
	}

	public static String[] listCoil(String[] listIn) {
		String[] sliceCoil = new String[listIn.length];
		for (int w1 = 0; w1 < listIn.length; w1++) {
			ImagePlus imp1 = UtilAyv.openImageNoDisplay(listIn[w1], true);
			String sliceCoil1 = ReadDicom.readDicomParameter(imp1, MyConst.DICOM_COIL);
			sliceCoil[w1] = sliceCoil1;
		}
		return sliceCoil;
	}

	/**
	 * Ricerca posizione ROI per calcolo uniformita'. Versione con Canny Edge
	 * Detector. Questa versione è da utilizzare per il fantoccio sferico
	 * 
	 * @param imp11
	 *            immagine in input
	 * @param info1
	 *            messaggio esplicativo
	 * @param autoCalled
	 *            true se chiamato in automatico
	 * @param step
	 *            true se in modo passo passo
	 * @param verbose
	 *            true se in modo verbose
	 * @param test
	 *            true se in test con junit, nessuna visualizzazione e richiesta
	 *            conferma
	 * @param fast
	 *            true se in modo batch
	 * @return
	 */
	public static double[] positionSniper(ImagePlus imp11, double maxFitError, double maxBubbleGapLimit, String info1,
			boolean autoCalled, boolean step, boolean demo0, boolean test, boolean fast, int timeout1) {

		// ================================================================================
		// Inizio calcoli geometrici
		// ================================================================================
		//

		Color colore1 = Color.red;
		Color colore2 = Color.green;
		Color colore3 = Color.red;

		// boolean debug = false;
		boolean manual = false;

		int xCenterCircle = 0;
		int yCenterCircle = 0;
		int xCenterCircleMan = 0;
		int yCenterCircleMan = 0;
		int diamCircleMan = 0;
		int xCenterCircleMan80 = 0;
		int yCenterCircleMan80 = 0;
		int diamCircleMan80 = 0;
		int diamCircle = 0;
		int xCenterMROI = 0;
		int yCenterMROI = 0;
		int diamMROI = 0;
		int xcorr = 0;
		int ycorr = 0;
		boolean showProfiles = false;

		int height = imp11.getHeight();
		int width = imp11.getWidth();
		int diamRoiMan = 173;

		ImageWindow iw11 = null;
		ImageWindow iw12 = null;
		if (demo0) {
			iw11 = imp11.getWindow();
		}

		Overlay over12 = new Overlay();

		// double dimPixel = ReadDicom.readDouble(
		// ReadDicom.readSubstring(ReadDicom.readDicomParameter(imp11,
		// MyConst.DICOM_PIXEL_SPACING), 1));

		MyCannyEdgeDetector mce = new MyCannyEdgeDetector();
		mce.setGaussianKernelRadius(2.0f);
		mce.setLowThreshold(15.0f);
		mce.setHighThreshold(16.0f);
		mce.setContrastNormalized(false);

		ImagePlus imp12 = mce.process(imp11);
		imp12.setOverlay(over12);
		imp12.show();
		if (step)
			MyLog.waitHere(info1);

		ImageStatistics stat12 = imp12.getStatistics();
		if (stat12.max < 255) {
			return null;
		}

		if (demo1)
			MyLog.waitHere("000\noutput CannyEdgeDetector");

		double[][] peaks1 = new double[4][1];
		double[][] peaks2 = new double[4][1];
		double[][] peaks3 = new double[4][1];
		double[][] peaks4 = new double[4][1];
		double[][] peaks5 = new double[4][1];
		double[][] peaks6 = new double[4][1];
		double[][] peaks7 = new double[4][1];
		double[][] peaks8 = new double[4][1];
		double[][] peaks9 = new double[4][1];
		double[][] peaks10 = new double[4][1];
		double[][] peaks11 = new double[4][1];
		double[][] peaks12 = new double[4][1];

		boolean strokewidth = true;
		double strWidth = 1.5;

		// ------ riadattamento da p10

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

		// int[] xPoints3 = null;
		// int[] yPoints3 = null;
		boolean vertical = false;
		boolean valido = true;
		for (int i1 = 0; i1 < 8; i1++) {

			// IJ.log("------------> i1= " + i1);

			xcoord[0] = vetx0[i1];
			ycoord[0] = vety0[i1];
			xcoord[1] = vetx1[i1];
			ycoord[1] = vety1[i1];
			imp12.setRoi(new Line(xcoord[0], ycoord[0], xcoord[1], ycoord[1]));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}

			if (i1 == 1)
				vertical = true;
			else
				vertical = false;

			if (demo0 && i1 == 0)
				showProfiles = true;
			else
				showProfiles = false;

			myPeaks = cannyProfileAnalyzer2(imp12, vetTitle[i1], showProfiles, demo0, debug, vertical, timeout);

			// myPeaks = profileAnalyzer(imp12, dimPixel, vetTitle[i1],
			// showProfiles, vertical, timeout);

			// String direction1 = ReadDicom.readDicomParameter(imp11,
			// MyConst.DICOM_IMAGE_ORIENTATION);
			// String direction2 = "1\0\0\01\0";

			if (myPeaks != null) {

				// per evitare le bolle d'aria escludero' il punto in alto per
				// l'immagine assiale ed il punto a sinistra dell'immagine
				// sagittale. Considero punto in alto quello con coordinata y <
				// mat/2 e come punto a sinistra quello con coordinata x < mat/2
				for (int i2 = 0; i2 < myPeaks[0].length; i2++) {
					valido = true;
					// MyLog.waitHere("direction1= " + direction1 + " i1= " +
					// i1);

					// if ((direction1.compareTo("0\\1\\0\\0\\0\\-1") == 0) &&
					// (i1 == 0)) {
					// // MyLog.waitHere("interdizione 0");
					//
					// if (((int) (myPeaks[0][i2]) < width / 2)) {
					// valido = false;
					// // MyLog.waitHere("linea orizzontale eliminato punto
					// // sx");
					// } else
					// ;
					// // MyLog.waitHere("linea orizzontale mantenuto punto
					// // dx");
					// }
					//
					// if ((direction1.compareTo("1\\0\\0\\0\\1\\0") == 0) &&
					// (i1 == 1)) {
					// // MyLog.waitHere("interdizione 1");
					// if (((int) (myPeaks[1][i2]) < height / 2)) {
					// valido = false;
					// // MyLog.waitHere("linea verticale eliminato punto
					// // sup");
					// } else
					// ;
					// // MyLog.waitHere("linea verticale mantenuto punto
					// // inf");
					// }

					if (valido) {

						count++;
						myXpoints[count] = (int) (myPeaks[3][i2]);
						myYpoints[count] = (int) (myPeaks[4][i2]);
						ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[3][i2]), (int) (myPeaks[4][i2]), colore1,
								1);
						ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[3][i2]), (int) (myPeaks[4][i2]), colore1,
								0);
						imp12.updateAndDraw();
						ImageUtils.imageToFront(imp12);
					}
					// MyLog.logVector(myXpoints, "myXpoints");
					// MyLog.logVector(myYpoints, "myYpoints");
				}
			}
		}
		if (demo0)
			MyLog.waitHere("Si tracciano ulteriori linee", debug, timeout);

		int[] xPoints3 = new int[1];
		int[] yPoints3 = new int[1];
		if (count >= 1) {
			xPoints3 = new int[count];
			yPoints3 = new int[count];

			count++;
			xPoints3 = new int[count];
			yPoints3 = new int[count];

			for (int i3 = 0; i3 < count; i3++) {
				xPoints3[i3] = myXpoints[i3];
				yPoints3[i3] = myYpoints[i3];
			}
		}

		over12.clear();

		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------
		// MyLog.waitHere("uno");

		// if (xPoints3.length < 3 || test) {
		// UtilAyv.showImageMaximized(imp11);
		// MyLog.waitHere(
		// "Non si riescono a determinare le coordinate di almeno 3 punti del
		// cerchio \n posizionare manualmente una ROI circolare di diametro
		// uguale al fantoccio e\n premere OK",
		// debug, timeout1);
		// manual = true;
		// }

		if (!manual) {

			PointRoi pr12 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
			pr12.setPointType(2);
			pr12.setSize(2);
			imp12.setRoi(pr12);

			if (demo0) {
				ImageUtils.addOverlayRoi(imp12, colore1, 3.1);
				pr12.setPointType(2);
				pr12.setSize(2);

				// over12.addElement(imp12.getRoi());
				// over12.setStrokeColor(Color.green);
				// imp12.setOverlay(over12);
				// imp12.updateAndDraw();
				// MyLog.waitHere(listaMessaggi(5), debug, timeout1);
			}
			// ---------------------------------------------------
			// eseguo ora fitCircle per trovare centro e dimensione del
			// fantoccio
			// ---------------------------------------------------
			if (xPoints3.length < 3) {
				ImageWindow iw112 = imp12.getWindow();
				if (iw112 != null)
					iw112.dispose();
				ImageWindow iw111 = imp11.getWindow();
				if (iw111 != null)
					iw111.dispose();

				return null;
			}
			ImageUtils.fitCircle(imp12);
			Boolean demo2 = true;
			Boolean demo3 = true;
			if (demo2) {
				imp12.getRoi().setStrokeColor(colore1);
				over12.addElement(imp12.getRoi());
			}

			if (demo3)
				MyLog.waitHere("La circonferenza risultante dal fit e' mostrata in rosso", debug, timeout1);
			Rectangle boundRec = imp12.getProcessor().getRoi();
			xCenterCircle = Math.round(boundRec.x + boundRec.width / 2);
			yCenterCircle = Math.round(boundRec.y + boundRec.height / 2);
			diamCircle = boundRec.width;
			// if (!manualOverride)
			// writeStoredRoiData(boundRec);

			MyCircleDetector.drawCenter(imp12, over12, xCenterCircle, yCenterCircle, colore3);
			if (demo1)
				MyLog.waitHere("002\nLa circonferenza risultante dal fit e' mostrata in rosso ed ha  \nxCenterCircle= "
						+ xCenterCircle + "  yCenterCircle= " + yCenterCircle + " diamCircle= " + diamCircle);

			// ----------------------------------------------------------
			// Misuro l'errore sul fit rispetto ai punti imposti
			// -----------------------------------------------------------
			double[] vetDist = new double[xPoints3.length];
			double sumError = 0;
			for (int i1 = 0; i1 < xPoints3.length; i1++) {
				vetDist[i1] = pointCirconferenceDistance(xPoints3[i1], yPoints3[i1], xCenterCircle, yCenterCircle,
						diamCircle / 2);
				sumError += Math.abs(vetDist[i1]);
			}
			if (sumError > maxFitError) {

				// -------------------------------------------------------------
				// disegno il cerchio ed i punti, in modo da date un feedback
				// grafico al messaggio di eccessivo errore nel fit
				// -------------------------------------------------------------
				UtilAyv.showImageMaximized(imp11);
				over12.clear();
				imp11.setOverlay(over12);
				imp11.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2, yCenterCircle - diamCircle / 2, diamCircle,
						diamCircle));
				imp11.getRoi().setStrokeColor(colore1);
				over12.addElement(imp11.getRoi());
				imp11.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp11.getRoi().setStrokeColor(colore2);
				over12.addElement(imp11.getRoi());
				imp11.deleteRoi();
				MyLog.waitHere(listaMessaggi(16), debug, timeout1);
				MyLog.waitHere("maxFitError");
				manual = true;
			}

			//
			// ----------------------------------------------------------
			// disegno la ROI del centro, a solo scopo dimostrativo !
			// ----------------------------------------------------------
			//

			if (demo0) {
				MyCircleDetector.drawCenter(imp12, over12, xCenterCircle, yCenterCircle, colore1);
				MyLog.waitHere(listaMessaggi(7), debug, timeout1);

			}

			Boolean noDebug = false;
			// =============================================================
			// COMPENSAZIONE PER EVENTUALE BOLLA D'ARIA NEL FANTOCCIO
			// la mantengo, anche se nei fantocci attuali non c'è bolla. Notato
			// sporadici interventi con spostamenti limitati.
			// ==============================================================

			// Traccio nuovamente le bisettrici verticale ed orizzontale, solo
			// che anziche' essere sul centro dell'immagine, ora sono poste sul
			// centro del cerchio circoscritto al fantoccio

			// BISETTRICE VERTICALE FANTOCCIO

			imp12.setRoi(new Line(xCenterCircle, 0, xCenterCircle, height));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks9 = cannyProfileAnalyzer2(imp12, "BISETTRICE VERTICALE FANTOCCIO", showProfiles, false, false, false,
					1);

			// MyLog.logMatrix(peaks9, "peaks9");
			// MyLog.waitHere();

			// PLOTTAGGIO PUNTI

			double gapVert = 0;
			if (peaks9 != null) {
				ImageUtils.plotPoints(imp12, over12, peaks9);
				gapVert = diamCircle / 2 - (yCenterCircle - peaks9[4][0]);
			}

			// BISETTRICE ORIZZONTALE FANTOCCIO

			imp12.setRoi(new Line(0, yCenterCircle, width, yCenterCircle));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks10 = cannyProfileAnalyzer2(imp12, "BISETTRICE ORIZZONTALE FANTOCCIO", showProfiles, false, false,
					false, 1);

			double gapOrizz = 0;
			if (peaks10 != null) {
				ImageUtils.plotPoints(imp12, over12, peaks10);
				gapOrizz = diamCircle / 2 - (xCenterCircle - peaks10[3][0]);
			}

			if (demo0)
				MyLog.waitHere(listaMessaggi(8) + maxBubbleGapLimit, noDebug, timeout1);

			// Effettuo in ogni caso la correzione, solo che in assenza di bolla
			// d'aria la correzione sara' irrisoria, in presenza di bolla la
			// correzione sara' apprezzabile

			if (gapOrizz > gapVert) {
				xcorr = (int) gapOrizz / 2;
			} else {
				ycorr = (int) gapVert / 2;
			}

			if ((xcorr + ycorr) > maxBubbleGapLimit)
				MyLog.waitHere("xcorr= " + xcorr + " ycorr= " + ycorr);

			// ---------------------------------------
			// qesto e' il risultato della nostra correzione e saranno i dati
			// della MROI
			diamMROI = (int) Math.round(diamCircle * MyConst.P3_AREA_PERC_80_DIAM);

			xCenterMROI = xCenterCircle + xcorr;
			yCenterMROI = yCenterCircle + ycorr;

			if (demo1)
				MyLog.waitHere("0015\nCorrezioni per bolla d'aria: \nxcorr= " + xcorr + " ycorr= " + ycorr
						+ "\nDATI MROI 80% xCenterMROI= " + xCenterMROI + " yCenterMROI= " + yCenterMROI + " diamMROI= "
						+ diamMROI);

			// ---------------------------------------
			// verifico ora che l'entita' della bolla non sia cosi' grande da
			// portare l'area MROI troppo a contatto del profilo fantoccio
			// calcolato sulle bisettrici
			// ---------------------------------------
			imp12.setRoi(new Line(xCenterMROI, 0, xCenterMROI, height));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks11 = cannyProfileAnalyzer2(imp12, "BISETTRICE VERTICALE MROI", showProfiles, false, false, false, 1);
			if (peaks11 != null) {
				// PLOTTAGGIO PUNTI
				ImageUtils.plotPoints(imp12, over12, peaks11);
			}

			imp12.setRoi(new Line(0, yCenterMROI, width, yCenterMROI));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks12 = cannyProfileAnalyzer2(imp12, "BISETTRICE ORIZZONTALE MROI", showProfiles, false, false, false, 1);
			if (peaks12 != null) {
				// PLOTTAGGIO PUNTI
				ImageUtils.plotPoints(imp12, over12, peaks12);
			}

			double d1 = maxBubbleGapLimit;
			double d2 = maxBubbleGapLimit;
			double d3 = maxBubbleGapLimit;
			double d4 = maxBubbleGapLimit;
			double dMin = 9999;

			// verticale
			if (peaks11 != null) {
				d1 = -(peaks11[4][0] - (yCenterMROI - diamMROI / 2));
				d2 = peaks11[4][1] - (yCenterMROI + diamMROI / 2);
			}
			// orizzontale
			if (peaks12 != null) {
				d3 = -(peaks12[3][0] - (xCenterMROI - diamMROI / 2));
				d4 = peaks12[3][1] - (xCenterMROI + diamMROI / 2);
			}

			dMin = Math.min(dMin, d1);
			dMin = Math.min(dMin, d2);
			dMin = Math.min(dMin, d3);
			dMin = Math.min(dMin, d4);

			if (dMin < maxBubbleGapLimit) {
				if (noDebug)
					MyLog.waitHere("dMin= " + dMin + " maxBubbleGapLimit= " + maxBubbleGapLimit);
				manual = true;
				// -------------------------------------------------------------
				// disegno il cerchio ed i punti, in modo da date un feedback
				// grafico al messaggio di eccessivo errore da bolla d'aria
				// -------------------------------------------------------------
				// UtilAyv.showImageMaximized(imp11);
				over12.clear();
				imp11.setOverlay(over12);
				imp11.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2, yCenterCircle - diamCircle / 2, diamCircle,
						diamCircle));
				imp11.getRoi().setStrokeColor(colore1);
				over12.addElement(imp11.getRoi());
				imp11.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp11.getRoi().setStrokeColor(colore2);
				over12.addElement(imp11.getRoi());
				imp11.deleteRoi();
				imp11.setRoi(new OvalRoi(xCenterMROI - diamMROI / 2, yCenterMROI - diamMROI / 2, diamMROI, diamMROI));
				imp11.getRoi().setStrokeColor(colore1);
				over12.addElement(imp11.getRoi());
				imp11.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp11.getRoi().setStrokeColor(colore2);
				over12.addElement(imp11.getRoi());
				imp11.deleteRoi();
				if (demo1)
					MyLog.waitHere("010\nBOLLA D'ARIA ECCESSIVA");

				if (noDebug)
					MyLog.waitHere(listaMessaggi(51) + " dMin= " + dMin, debug, timeout1);
			} else {
				imp12.setRoi(new OvalRoi(xCenterMROI - diamMROI / 2, yCenterMROI - diamMROI / 2, diamMROI, diamMROI));
				Rectangle boundingRectangle2 = imp12.getProcessor().getRoi();
				diamMROI = (int) boundingRectangle2.width;
				xCenterMROI = boundingRectangle2.x + boundingRectangle2.width / 2;
				yCenterMROI = boundingRectangle2.y + boundingRectangle2.height / 2;
				// imp12.killRoi();
				if (demo1)
					MyLog.waitHere("006\nMROI 80% in giallo");
			}
		}
		manual = false;
		if (manual) {
			MyLog.waitHere("INTERVENTO MANUALE");
			// ==================================================================
			// INTERVENTO MANUALE PER CASI DISPERATI, SAN GENNARO PENSACI TU
			// ==================================================================
			fast = false;

			over12.clear();
			imp12.close();
			if (!imp11.isVisible())
				UtilAyv.showImageMaximized(imp11);
			imp11.setRoi(new OvalRoi(width / 2 - diamRoiMan / 2, height / 2 - diamRoiMan / 2, diamRoiMan, diamRoiMan));
			MyLog.waitHere(listaMessaggi(14), debug, timeout1);

			if (timeout1 > 0) {
				// in questo caso (simulazione in corso) devo simulare
				// l'intervento manuale dell'operatore. Lo simulo impostando una
				// nuova posizione della Roi con Roi.setLocation
				imp11.getRoi().setLocation(40, 29);
				imp11.updateAndDraw();
				// MyLog.waitHere();
			}

			Rectangle boundRec11 = imp11.getProcessor().getRoi();
			xCenterCircleMan = Math.round(boundRec11.x + boundRec11.width / 2);
			yCenterCircleMan = Math.round(boundRec11.y + boundRec11.height / 2);
			diamCircleMan = boundRec11.width;
			diamCircleMan80 = (int) Math.round(diamCircleMan * MyConst.P3_AREA_PERC_80_DIAM);
			imp11.setRoi(new OvalRoi(xCenterCircleMan - diamCircleMan80 / 2, yCenterCircleMan - diamCircleMan80 / 2,
					diamCircleMan80, diamCircleMan80));
			MyLog.waitHere(listaMessaggi(18), debug, timeout1);

			Rectangle boundRec111 = imp11.getProcessor().getRoi();
			xCenterCircleMan80 = Math.round(boundRec111.x + boundRec111.width / 2);
			yCenterCircleMan80 = Math.round(boundRec111.y + boundRec111.height / 2);
			diamCircleMan80 = boundRec111.width;

			// carico qui i dati dell'avvenuto posizionamento manuale
			xCenterCircle = xCenterCircleMan;
			yCenterCircle = yCenterCircleMan;
			diamCircle = diamCircleMan;

			xCenterMROI = xCenterCircleMan80;
			yCenterMROI = yCenterCircleMan80;
			diamMROI = diamCircleMan80;
			imp12.setRoi(new OvalRoi(xCenterMROI, yCenterMROI, diamMROI, diamMROI));
		}

		if (demo0) {
			imp12.updateAndDraw();
			imp12.getRoi().setStrokeColor(colore2);
			over12.addElement(imp12.getRoi());
			MyLog.waitHere(listaMessaggi(9), debug, timeout1);
			MyCircleDetector.drawCenter(imp12, over12, xCenterCircle + xcorr, yCenterCircle + ycorr, colore2);
			MyLog.waitHere(listaMessaggi(10), debug, timeout1);
		}
		ImageWindow ww11 = imp11.getWindow();
		if (ww11 != null)
			ww11.dispose();
		ImageWindow ww12 = imp12.getWindow();
		if (ww12 != null)
			ww12.dispose();

		over12.clear();
		imp12.close();
		imp11.deleteRoi();
		imp11.updateImage();

		double[] out2 = new double[6];
		out2[0] = xCenterCircle;
		out2[1] = yCenterCircle;
		out2[2] = diamCircle;
		out2[3] = xCenterMROI;
		out2[4] = yCenterMROI;
		out2[5] = diamMROI;
		if (demo1)
			MyLog.waitHere("020\nFine PositionSearch11");
		return out2;
	}

	/**
	 * Ricerca posizione ROI per calcolo uniformita'. Versione con Canny Edge
	 * Detector
	 * 
	 * @param imp11
	 *            immagine in input
	 * @param info1
	 *            messaggio esplicativo
	 * @param autoCalled
	 *            true se chiamato in automatico
	 * @param step
	 *            true se in modo passo passo
	 * @param verbose
	 *            true se in modo verbose
	 * @param test
	 *            true se in test con junit, nessuna visualizzazione e richiesta
	 *            conferma
	 * @param fast
	 *            true se in modo batch
	 * @return
	 */
	public static double[] positionSearch11(ImagePlus imp11, double maxFitError, double maxBubbleGapLimit, String info1,
			boolean autoCalled, boolean step, boolean demo0, boolean test, boolean fast, int timeout1) {

		// MyLog.waitHere("autoCalled= "+autoCalled+"\nstep= "+step+"\ndemo=
		// "+demo+"\ntest= "+test+
		// "\nfast= "+fast);

		//
		// ================================================================================
		// Inizio calcoli geometrici
		// ================================================================================
		//

		Color colore1 = Color.red;
		Color colore2 = Color.green;
		Color colore3 = Color.red;

		// boolean debug = false;
		boolean manual = false;

		int xCenterCircle = 0;
		int yCenterCircle = 0;
		int xCenterCircleMan = 0;
		int yCenterCircleMan = 0;
		int diamCircleMan = 0;
		int xCenterCircleMan80 = 0;
		int yCenterCircleMan80 = 0;
		int diamCircleMan80 = 0;
		int diamCircle = 0;
		int xCenterMROI = 0;
		int yCenterMROI = 0;
		int diamMROI = 0;
		int xcorr = 0;
		int ycorr = 0;
		boolean showProfiles = false;

		int height = imp11.getHeight();
		int width = imp11.getWidth();
		int diamRoiMan = 173;

		ImageWindow iw11 = null;
		ImageWindow iw12 = null;
		if (demo0) {
			iw11 = imp11.getWindow();
		}

		Overlay over12 = new Overlay();

		double dimPixel = ReadDicom.readDouble(
				ReadDicom.readSubstring(ReadDicom.readDicomParameter(imp11, MyConst.DICOM_PIXEL_SPACING), 1));

		MyCannyEdgeDetector mce = new MyCannyEdgeDetector();
		mce.setGaussianKernelRadius(2.0f);
		mce.setLowThreshold(15.0f);
		mce.setHighThreshold(16.0f);
		mce.setContrastNormalized(false);

		ImagePlus imp12 = mce.process(imp11);
		imp12.setOverlay(over12);
		// UtilAyv.showImageMaximized2(imp12);

		ImageStatistics stat12 = imp12.getStatistics();
		if (stat12.max < 255) {
			return null;
		}

		if (demo1)
			MyLog.waitHere("000\noutput CannyEdgeDetector");

		double[][] peaks1 = new double[4][1];
		double[][] peaks2 = new double[4][1];
		double[][] peaks3 = new double[4][1];
		double[][] peaks4 = new double[4][1];
		double[][] peaks5 = new double[4][1];
		double[][] peaks6 = new double[4][1];
		double[][] peaks7 = new double[4][1];
		double[][] peaks8 = new double[4][1];
		double[][] peaks9 = new double[4][1];
		double[][] peaks10 = new double[4][1];
		double[][] peaks11 = new double[4][1];
		double[][] peaks12 = new double[4][1];

		boolean strokewidth = true;
		double strWidth = 1.5;

		// ------ riadattamento da p10

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

		// int[] xPoints3 = null;
		// int[] yPoints3 = null;
		boolean vertical = false;
		boolean valido = true;
		for (int i1 = 0; i1 < 8; i1++) {

			// IJ.log("------------> i1= " + i1);

			xcoord[0] = vetx0[i1];
			ycoord[0] = vety0[i1];
			xcoord[1] = vetx1[i1];
			ycoord[1] = vety1[i1];
			imp12.setRoi(new Line(xcoord[0], ycoord[0], xcoord[1], ycoord[1]));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}

			if (i1 == 1)
				vertical = true;
			else
				vertical = false;

			if (demo0 && i1 == 0)
				showProfiles = true;
			else
				showProfiles = false;

			myPeaks = cannyProfileAnalyzer(imp12, dimPixel, vetTitle[i1], showProfiles, demo0, debug, vertical,
					timeout);

			// myPeaks = profileAnalyzer(imp12, dimPixel, vetTitle[i1],
			// showProfiles, vertical, timeout);

			String direction1 = ReadDicom.readDicomParameter(imp11, MyConst.DICOM_IMAGE_ORIENTATION);
			String direction2 = "1\0\0\01\0";

			if (myPeaks != null) {

				// per evitare le bolle d'aria escludero' il punto in alto per
				// l'immagine assiale ed il punto a sinistra dell'immagine
				// sagittale. Considero punto in alto quello con coordinata y <
				// mat/2 e come punto a sinistra quello con coordinata x < mat/2
				for (int i2 = 0; i2 < myPeaks[0].length; i2++) {
					valido = true;
					// MyLog.waitHere("direction1= " + direction1 + " i1= " +
					// i1);

					if ((direction1.compareTo("0\\1\\0\\0\\0\\-1") == 0) && (i1 == 0)) {
						// MyLog.waitHere("interdizione 0");

						if (((int) (myPeaks[0][i2]) < width / 2)) {
							valido = false;
							// MyLog.waitHere("linea orizzontale eliminato punto
							// sx");
						} else
							;
						// MyLog.waitHere("linea orizzontale mantenuto punto
						// dx");
					}

					if ((direction1.compareTo("1\\0\\0\\0\\1\\0") == 0) && (i1 == 1)) {
						// MyLog.waitHere("interdizione 1");
						if (((int) (myPeaks[1][i2]) < height / 2)) {
							valido = false;
							// MyLog.waitHere("linea verticale eliminato punto
							// sup");
						} else
							;
						// MyLog.waitHere("linea verticale mantenuto punto
						// inf");
					}

					if (valido) {

						count++;
						myXpoints[count] = (int) (myPeaks[3][i2]);
						myYpoints[count] = (int) (myPeaks[4][i2]);
						// ImageUtils.plotPoints(imp12, over12, (int)
						// (myPeaks[3][i2]), (int) (myPeaks[4][i2]), colore1,
						// 1);
						// ImageUtils.plotPoints(imp12, over12, (int)
						// (myPeaks[3][i2]), (int) (myPeaks[4][i2]), colore1,
						// 0);
						// imp12.updateAndDraw();
						// ImageUtils.imageToFront(imp12);
					}
					// MyLog.logVector(myXpoints, "myXpoints");
					// MyLog.logVector(myYpoints, "myYpoints");
				}
			}
		}
		if (demo0)
			MyLog.waitHere("Si tracciano ulteriori linee", debug, timeout);

		int[] xPoints3 = new int[1];
		int[] yPoints3 = new int[1];
		if (count >= 1) {
			xPoints3 = new int[count];
			yPoints3 = new int[count];

			count++;
			xPoints3 = new int[count];
			yPoints3 = new int[count];

			for (int i3 = 0; i3 < count; i3++) {
				xPoints3[i3] = myXpoints[i3];
				yPoints3[i3] = myYpoints[i3];
			}
		}
		over12.clear();

		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------
		// MyLog.waitHere("uno");

		// if (xPoints3.length < 3 || test) {
		// UtilAyv.showImageMaximized(imp11);
		// MyLog.waitHere(
		// "Non si riescono a determinare le coordinate di almeno 3 punti del
		// cerchio \n posizionare manualmente una ROI circolare di diametro
		// uguale al fantoccio e\n premere OK",
		// debug, timeout1);
		// manual = true;
		// }

		if (!manual) {

			PointRoi pr12 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
			pr12.setPointType(2);
			pr12.setSize(2);
			imp12.setRoi(pr12);

			if (demo0) {
				ImageUtils.addOverlayRoi(imp12, colore1, 3.1);
				pr12.setPointType(2);
				pr12.setSize(2);

				// over12.addElement(imp12.getRoi());
				// over12.setStrokeColor(Color.green);
				// imp12.setOverlay(over12);
				// imp12.updateAndDraw();
				// MyLog.waitHere(listaMessaggi(5), debug, timeout1);
			}
			// ---------------------------------------------------
			// eseguo ora fitCircle per trovare centro e dimensione del
			// fantoccio
			// ---------------------------------------------------
			if (xPoints3.length < 3) {
				ImageWindow iw112 = imp12.getWindow();
				if (iw112 != null)
					iw112.dispose();
				ImageWindow iw111 = imp11.getWindow();
				if (iw111 != null)
					iw111.dispose();

				return null;
			}
			ImageUtils.fitCircle(imp12);
			Boolean demo2 = true;
			Boolean demo3 = false;
			if (demo2) {
				imp12.getRoi().setStrokeColor(colore1);
				over12.addElement(imp12.getRoi());
			}

			if (demo3)
				MyLog.waitHere("La circonferenza risultante dal fit e' mostrata in rosso", debug, timeout1);
			Rectangle boundRec = imp12.getProcessor().getRoi();
			xCenterCircle = Math.round(boundRec.x + boundRec.width / 2);
			yCenterCircle = Math.round(boundRec.y + boundRec.height / 2);
			diamCircle = boundRec.width;
			// if (!manualOverride)
			// writeStoredRoiData(boundRec);

			MyCircleDetector.drawCenter(imp12, over12, xCenterCircle, yCenterCircle, colore3);
			if (demo1)
				MyLog.waitHere("002\nLa circonferenza risultante dal fit e' mostrata in rosso ed ha  \nxCenterCircle= "
						+ xCenterCircle + "  yCenterCircle= " + yCenterCircle + " diamCircle= " + diamCircle);

			// ----------------------------------------------------------
			// Misuro l'errore sul fit rispetto ai punti imposti
			// -----------------------------------------------------------
			double[] vetDist = new double[xPoints3.length];
			double sumError = 0;
			for (int i1 = 0; i1 < xPoints3.length; i1++) {
				vetDist[i1] = pointCirconferenceDistance(xPoints3[i1], yPoints3[i1], xCenterCircle, yCenterCircle,
						diamCircle / 2);
				sumError += Math.abs(vetDist[i1]);
			}
			if (sumError > maxFitError) {

				// -------------------------------------------------------------
				// disegno il cerchio ed i punti, in modo da date un feedback
				// grafico al messaggio di eccessivo errore nel fit
				// -------------------------------------------------------------
				UtilAyv.showImageMaximized(imp11);
				over12.clear();
				imp11.setOverlay(over12);
				imp11.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2, yCenterCircle - diamCircle / 2, diamCircle,
						diamCircle));
				imp11.getRoi().setStrokeColor(colore1);
				over12.addElement(imp11.getRoi());
				imp11.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp11.getRoi().setStrokeColor(colore2);
				over12.addElement(imp11.getRoi());
				imp11.deleteRoi();
				MyLog.waitHere(listaMessaggi(16), debug, timeout1);
				MyLog.waitHere("maxFitError");
				manual = true;
			}

			//
			// ----------------------------------------------------------
			// disegno la ROI del centro, a solo scopo dimostrativo !
			// ----------------------------------------------------------
			//

			if (demo0) {
				MyCircleDetector.drawCenter(imp12, over12, xCenterCircle, yCenterCircle, colore1);
				MyLog.waitHere(listaMessaggi(7), debug, timeout1);

			}

			Boolean noDebug = false;
			// =============================================================
			// COMPENSAZIONE PER EVENTUALE BOLLA D'ARIA NEL FANTOCCIO
			// la mantengo, anche se nei fantocci attuali non c'è bolla. Notato
			// sporadici interventi con spostamenti limitati.
			// ==============================================================

			// Traccio nuovamente le bisettrici verticale ed orizzontale, solo
			// che anziche' essere sul centro dell'immagine, ora sono poste sul
			// centro del cerchio circoscritto al fantoccio

			// BISETTRICE VERTICALE FANTOCCIO

			imp12.setRoi(new Line(xCenterCircle, 0, xCenterCircle, height));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks9 = cannyProfileAnalyzer(imp12, dimPixel, "BISETTRICE VERTICALE FANTOCCIO", showProfiles, false, false,
					false, 1);

			// MyLog.logMatrix(peaks9, "peaks9");
			// MyLog.waitHere();

			// PLOTTAGGIO PUNTI

			double gapVert = 0;
			if (peaks9 != null) {
				ImageUtils.plotPoints(imp12, over12, peaks9);
				gapVert = diamCircle / 2 - (yCenterCircle - peaks9[4][0]);
			}

			// BISETTRICE ORIZZONTALE FANTOCCIO

			imp12.setRoi(new Line(0, yCenterCircle, width, yCenterCircle));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks10 = cannyProfileAnalyzer(imp12, dimPixel, "BISETTRICE ORIZZONTALE FANTOCCIO", showProfiles, false,
					false, false, 1);

			double gapOrizz = 0;
			if (peaks10 != null) {
				ImageUtils.plotPoints(imp12, over12, peaks10);
				gapOrizz = diamCircle / 2 - (xCenterCircle - peaks10[3][0]);
			}

			if (demo0)
				MyLog.waitHere(listaMessaggi(8) + maxBubbleGapLimit, noDebug, timeout1);

			// Effettuo in ogni caso la correzione, solo che in assenza di bolla
			// d'aria la correzione sara' irrisoria, in presenza di bolla la
			// correzione sara' apprezzabile

			if (gapOrizz > gapVert) {
				xcorr = (int) gapOrizz / 2;
			} else {
				ycorr = (int) gapVert / 2;
			}

			if ((xcorr + ycorr) > maxBubbleGapLimit)
				MyLog.waitHere("xcorr= " + xcorr + " ycorr= " + ycorr);

			// ---------------------------------------
			// qesto e' il risultato della nostra correzione e saranno i dati
			// della MROI
			diamMROI = (int) Math.round(diamCircle * MyConst.P3_AREA_PERC_80_DIAM);

			xCenterMROI = xCenterCircle + xcorr;
			yCenterMROI = yCenterCircle + ycorr;

			if (demo1)
				MyLog.waitHere("0015\nCorrezioni per bolla d'aria: \nxcorr= " + xcorr + " ycorr= " + ycorr
						+ "\nDATI MROI 80% xCenterMROI= " + xCenterMROI + " yCenterMROI= " + yCenterMROI + " diamMROI= "
						+ diamMROI);

			// ---------------------------------------
			// verifico ora che l'entita' della bolla non sia cosi' grande da
			// portare l'area MROI troppo a contatto del profilo fantoccio
			// calcolato sulle bisettrici
			// ---------------------------------------
			imp12.setRoi(new Line(xCenterMROI, 0, xCenterMROI, height));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks11 = cannyProfileAnalyzer(imp12, dimPixel, "BISETTRICE VERTICALE MROI", showProfiles, false, false,
					false, 1);
			if (peaks11 != null) {
				// PLOTTAGGIO PUNTI
				ImageUtils.plotPoints(imp12, over12, peaks11);
			}

			imp12.setRoi(new Line(0, yCenterMROI, width, yCenterMROI));
			if (demo0) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}
			peaks12 = cannyProfileAnalyzer(imp12, dimPixel, "BISETTRICE ORIZZONTALE MROI", showProfiles, false, false,
					false, 1);
			if (peaks12 != null) {
				// PLOTTAGGIO PUNTI
				ImageUtils.plotPoints(imp12, over12, peaks12);
			}

			double d1 = maxBubbleGapLimit;
			double d2 = maxBubbleGapLimit;
			double d3 = maxBubbleGapLimit;
			double d4 = maxBubbleGapLimit;
			double dMin = 9999;

			// verticale
			if (peaks11 != null) {
				d1 = -(peaks11[4][0] - (yCenterMROI - diamMROI / 2));
				d2 = peaks11[4][1] - (yCenterMROI + diamMROI / 2);
			}
			// orizzontale
			if (peaks12 != null) {
				d3 = -(peaks12[3][0] - (xCenterMROI - diamMROI / 2));
				d4 = peaks12[3][1] - (xCenterMROI + diamMROI / 2);
			}

			dMin = Math.min(dMin, d1);
			dMin = Math.min(dMin, d2);
			dMin = Math.min(dMin, d3);
			dMin = Math.min(dMin, d4);

			if (dMin < maxBubbleGapLimit) {
				if (noDebug)
					MyLog.waitHere("dMin= " + dMin + " maxBubbleGapLimit= " + maxBubbleGapLimit);
				manual = true;
				// -------------------------------------------------------------
				// disegno il cerchio ed i punti, in modo da date un feedback
				// grafico al messaggio di eccessivo errore da bolla d'aria
				// -------------------------------------------------------------
				// UtilAyv.showImageMaximized(imp11);
				over12.clear();
				imp11.setOverlay(over12);
				imp11.setRoi(new OvalRoi(xCenterCircle - diamCircle / 2, yCenterCircle - diamCircle / 2, diamCircle,
						diamCircle));
				imp11.getRoi().setStrokeColor(colore1);
				over12.addElement(imp11.getRoi());
				imp11.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp11.getRoi().setStrokeColor(colore2);
				over12.addElement(imp11.getRoi());
				imp11.deleteRoi();
				imp11.setRoi(new OvalRoi(xCenterMROI - diamMROI / 2, yCenterMROI - diamMROI / 2, diamMROI, diamMROI));
				imp11.getRoi().setStrokeColor(colore1);
				over12.addElement(imp11.getRoi());
				imp11.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp11.getRoi().setStrokeColor(colore2);
				over12.addElement(imp11.getRoi());
				imp11.deleteRoi();
				if (demo1)
					MyLog.waitHere("010\nBOLLA D'ARIA ECCESSIVA");

				if (noDebug)
					MyLog.waitHere(listaMessaggi(51) + " dMin= " + dMin, debug, timeout1);
			} else {
				imp12.setRoi(new OvalRoi(xCenterMROI - diamMROI / 2, yCenterMROI - diamMROI / 2, diamMROI, diamMROI));
				Rectangle boundingRectangle2 = imp12.getProcessor().getRoi();
				diamMROI = (int) boundingRectangle2.width;
				xCenterMROI = boundingRectangle2.x + boundingRectangle2.width / 2;
				yCenterMROI = boundingRectangle2.y + boundingRectangle2.height / 2;
				// imp12.killRoi();
				if (demo1)
					MyLog.waitHere("006\nMROI 80% in giallo");
			}
		}
		manual = false;
		if (manual) {
			MyLog.waitHere("INTERVENTO MANUALE");
			// ==================================================================
			// INTERVENTO MANUALE PER CASI DISPERATI, SAN GENNARO PENSACI TU
			// ==================================================================
			fast = false;

			over12.clear();
			imp12.close();
			if (!imp11.isVisible())
				UtilAyv.showImageMaximized(imp11);
			imp11.setRoi(new OvalRoi(width / 2 - diamRoiMan / 2, height / 2 - diamRoiMan / 2, diamRoiMan, diamRoiMan));
			MyLog.waitHere(listaMessaggi(14), debug, timeout1);

			if (timeout1 > 0) {
				// in questo caso (simulazione in corso) devo simulare
				// l'intervento manuale dell'operatore. Lo simulo impostando una
				// nuova posizione della Roi con Roi.setLocation
				imp11.getRoi().setLocation(40, 29);
				imp11.updateAndDraw();
				// MyLog.waitHere();
			}

			Rectangle boundRec11 = imp11.getProcessor().getRoi();
			xCenterCircleMan = Math.round(boundRec11.x + boundRec11.width / 2);
			yCenterCircleMan = Math.round(boundRec11.y + boundRec11.height / 2);
			diamCircleMan = boundRec11.width;
			diamCircleMan80 = (int) Math.round(diamCircleMan * MyConst.P3_AREA_PERC_80_DIAM);
			imp11.setRoi(new OvalRoi(xCenterCircleMan - diamCircleMan80 / 2, yCenterCircleMan - diamCircleMan80 / 2,
					diamCircleMan80, diamCircleMan80));
			MyLog.waitHere(listaMessaggi(18), debug, timeout1);

			Rectangle boundRec111 = imp11.getProcessor().getRoi();
			xCenterCircleMan80 = Math.round(boundRec111.x + boundRec111.width / 2);
			yCenterCircleMan80 = Math.round(boundRec111.y + boundRec111.height / 2);
			diamCircleMan80 = boundRec111.width;

			// carico qui i dati dell'avvenuto posizionamento manuale
			xCenterCircle = xCenterCircleMan;
			yCenterCircle = yCenterCircleMan;
			diamCircle = diamCircleMan;

			xCenterMROI = xCenterCircleMan80;
			yCenterMROI = yCenterCircleMan80;
			diamMROI = diamCircleMan80;
			imp12.setRoi(new OvalRoi(xCenterMROI, yCenterMROI, diamMROI, diamMROI));
		}

		if (demo0) {
			imp12.updateAndDraw();
			imp12.getRoi().setStrokeColor(colore2);
			over12.addElement(imp12.getRoi());
			MyLog.waitHere(listaMessaggi(9), debug, timeout1);
			MyCircleDetector.drawCenter(imp12, over12, xCenterCircle + xcorr, yCenterCircle + ycorr, colore2);
			MyLog.waitHere(listaMessaggi(10), debug, timeout1);
		}
		ImageWindow ww11 = imp11.getWindow();
		if (ww11 != null)
			ww11.dispose();
		ImageWindow ww12 = imp12.getWindow();
		if (ww12 != null)
			ww12.dispose();

		over12.clear();
		imp12.close();
		imp11.deleteRoi();
		imp11.updateImage();

		double[] out2 = new double[6];
		out2[0] = xCenterCircle;
		out2[1] = yCenterCircle;
		out2[2] = diamCircle;
		out2[3] = xCenterMROI;
		out2[4] = yCenterMROI;
		out2[5] = diamMROI;
		if (demo1)
			MyLog.waitHere("020\nFine PositionSearch11");
		return out2;
	}

	/**
	 * Riceve una ImagePlus derivante da un CannyEdgeDetector con impostata una
	 * Line, restituisce le coordinate dei 2 picchi, se non sono esattamente 2
	 * restituisce null.
	 * 
	 * @param imp1
	 * @param dimPixel
	 * @param title
	 * @param showProfiles
	 * @param demo
	 * @param debug
	 * @return
	 */
	public static double[][] cannyProfileAnalyzer2(ImagePlus imp1, String title, boolean showProfiles, boolean demo,
			boolean debug, boolean vertical, int timeout) {

		double[][] profi3 = MyLine.decomposer(imp1);
		if (profi3 == null) {
			MyLog.waitHere("profi3 == null");
			return null;
		}
		int count1 = 0;
		boolean ready1 = false;
		double max1 = 0;
		for (int i1 = 0; i1 < profi3[0].length; i1++) {

			if (profi3[2][i1] > max1) {
				max1 = profi3[2][i1];
				ready1 = true;
			}
			if ((profi3[2][i1] == 0) && ready1) {
				max1 = 0;
				count1++;
				ready1 = false;
			}
		}
		// devo ora contare i pixel a 255 che ho trovato, ne accettero' solo 2,
		if (count1 != 2) {
			if (demo)
				MyLog.waitHere("" + title + " trovati un numero di punti diverso da 2, count= " + count1
						+ " scartiamo questi risultati");
			return null;
		}

		// peaks1 viene utilizzato in un altra routine, per cui gli elementi 0,
		// 1 e
		// ed 2 sono utilizzati per altro, li lascio a 0
		double[][] peaks1 = new double[6][count1];

		int count2 = 0;
		boolean ready2 = false;
		double max2 = 0;

		for (int i1 = 0; i1 < profi3[0].length; i1++) {

			if (profi3[2][i1] > max2) {
				peaks1[3][count2] = profi3[0][i1];
				peaks1[4][count2] = profi3[1][i1];
				max2 = profi3[2][i1];
				peaks1[5][count2] = max2;

				ready2 = true;
			}
			if ((profi3[2][i1] == 0) && ready2) {
				max2 = 0;
				count2++;
				ready2 = false;
			}
		}

		// ----------------------------------------
		// AGGIUNGO 1 AI PUNTI TROVATI
		// ---------------------------------------

		for (int i1 = 0; i1 < peaks1.length; i1++) {
			for (int i2 = 0; i2 < peaks1[0].length; i2++)
				if (peaks1[i1][i2] > 0)
					peaks1[i1][i2] = peaks1[i1][i2] + 1;
		}

		if (showProfiles) {
			double[] bx = new double[profi3[2].length];
			for (int i1 = 0; i1 < profi3[2].length; i1++) {
				bx[i1] = (double) i1;
			}

			double[] xPoints = new double[peaks1[0].length];
			double[] yPoints = new double[peaks1[0].length];
			double[] zPoints = new double[peaks1[0].length];
			for (int i1 = 0; i1 < peaks1[0].length; i1++) {
				xPoints[i1] = peaks1[3][i1];
				yPoints[i1] = peaks1[4][i1];
				zPoints[i1] = peaks1[5][i1];
			}

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

			MyLog.waitHere(listaMessaggi(3), debug, timeout);

		}

		//
		// Plot plot4 = new Plot("Profile", "X Axis", "Y Axis", bx, profi3[2]);
		// plot4.setLimits(0, bx.length + 10, 0, 300);
		// plot4.setSize(400, 200);
		// plot4.setColor(Color.red);
		// plot4.setLineWidth(2);
		// plot4.show();

		if (WindowManager.getFrame("Profile") != null) {
			IJ.selectWindow("Profile");
			IJ.run("Close");
		}

		// verifico di avere trovato un max di 2 picchi
		if (peaks1[2].length > 2)
			MyLog.waitHere(
					"Attenzione trovate troppe intersezioni col cerchio, cioe' " + peaks1[2].length + "  VERIFICARE");
		if (peaks1[2].length < 2)
			MyLog.waitHere(
					"Attenzione trovata una sola intersezione col cerchio, cioe' " + peaks1[2].length + "  VERIFICARE");

		// MyLog.logMatrix(peaks1, "peaks1 " + title);

		return peaks1;
	}

	/**
	 * Riceve una ImagePlus derivante da un CannyEdgeDetector con impostata una
	 * Line, restituisce le coordinate dei 2 picchi, se non sono esattamente 2
	 * restituisce null.
	 * 
	 * @param imp1
	 * @param dimPixel
	 * @param title
	 * @param showProfiles
	 * @param demo
	 * @param debug
	 * @return
	 */
	public static double[][] cannyProfileAnalyzer(ImagePlus imp1, double dimPixel, String title, boolean showProfiles,
			boolean demo, boolean debug, boolean vertical, int timeout) {

		double[][] profi3 = MyLine.decomposer(imp1);
		if (profi3 == null) {
			MyLog.waitHere("profi3 == null");
			return null;
		}
		int count1 = 0;
		boolean ready1 = false;
		double max1 = 0;
		for (int i1 = 0; i1 < profi3[0].length; i1++) {

			if (profi3[2][i1] > max1) {
				max1 = profi3[2][i1];
				ready1 = true;
			}
			if ((profi3[2][i1] == 0) && ready1) {
				max1 = 0;
				count1++;
				ready1 = false;
			}
		}
		// devo ora contare i pixel a 255 che ho trovato, ne accettero' solo 2,
		if (count1 != 2) {
			if (demo)
				MyLog.waitHere("" + title + " trovati un numero di punti diverso da 2, count= " + count1
						+ " scartiamo questi risultati");
			return null;
		}

		// peaks1 viene utilizzato in un altra routine, per cui gli elementi 0,
		// 1 e
		// ed 2 sono utilizzati per altro, li lascio a 0
		double[][] peaks1 = new double[6][count1];

		int count2 = 0;
		boolean ready2 = false;
		double max2 = 0;

		for (int i1 = 0; i1 < profi3[0].length; i1++) {

			if (profi3[2][i1] > max2) {
				peaks1[3][count2] = profi3[0][i1];
				peaks1[4][count2] = profi3[1][i1];
				max2 = profi3[2][i1];
				peaks1[5][count2] = max2;

				ready2 = true;
			}
			if ((profi3[2][i1] == 0) && ready2) {
				max2 = 0;
				count2++;
				ready2 = false;
			}
		}

		// ----------------------------------------
		// AGGIUNGO 1 AI PUNTI TROVATI
		// ---------------------------------------

		for (int i1 = 0; i1 < peaks1.length; i1++) {
			for (int i2 = 0; i2 < peaks1[0].length; i2++)
				if (peaks1[i1][i2] > 0)
					peaks1[i1][i2] = peaks1[i1][i2] + 1;
		}

		if (showProfiles) {
			double[] bx = new double[profi3[2].length];
			for (int i1 = 0; i1 < profi3[2].length; i1++) {
				bx[i1] = (double) i1;
			}

			double[] xPoints = new double[peaks1[0].length];
			double[] yPoints = new double[peaks1[0].length];
			double[] zPoints = new double[peaks1[0].length];
			for (int i1 = 0; i1 < peaks1[0].length; i1++) {
				xPoints[i1] = peaks1[3][i1];
				yPoints[i1] = peaks1[4][i1];
				zPoints[i1] = peaks1[5][i1];
			}

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

			MyLog.waitHere(listaMessaggi(3), debug, timeout);

		}

		//
		// Plot plot4 = new Plot("Profile", "X Axis", "Y Axis", bx, profi3[2]);
		// plot4.setLimits(0, bx.length + 10, 0, 300);
		// plot4.setSize(400, 200);
		// plot4.setColor(Color.red);
		// plot4.setLineWidth(2);
		// plot4.show();

		if (WindowManager.getFrame("Profile") != null) {
			IJ.selectWindow("Profile");
			IJ.run("Close");
		}

		// verifico di avere trovato un max di 2 picchi
		if (peaks1[2].length > 2)
			MyLog.waitHere(
					"Attenzione trovate troppe intersezioni col cerchio, cioe' " + peaks1[2].length + "  VERIFICARE");
		if (peaks1[2].length < 2)
			MyLog.waitHere(
					"Attenzione trovata una sola intersezione col cerchio, cioe' " + peaks1[2].length + "  VERIFICARE");

		// MyLog.logMatrix(peaks1, "peaks1 " + title);

		return peaks1;
	}

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

	/**
	 * Qui sono raggruppati tutti i messaggi del plugin, in questo modo e'
	 * facilitata la eventuale modifica / traduzione dei messaggi.
	 * 
	 * @param select
	 * @return
	 */
	public static String listaMessaggi(int select) {
		String[] lista = new String[100];
		// ---------+-----------------------------------------------------------+
		lista[0] = "L'immagine in input viene processata con il Canny Edge Detector";
		lista[1] = "L'immagine risultante e' una immagine ad 8 bit, con i soli valori \n"
				+ "0 e 255. Lo spessore del perimetro del cerchio e' di 1 pixel";
		lista[2] = "Si tracciano ulteriori linee, passanti per il centro dell'immagine, \n"
				+ "su queste linee si cercano le intersezioni con il cerchio";
		lista[3] = "Analizzando il profilo del segnale lungo la linea si ricavano \n"
				+ "le coordinate delle due intersezioni con la circonferenza.";
		lista[4] = "Per tenere conto delle possibili bolle d'aria del fantoccio, si \n"
				+ "escludono dalla bisettice orizzontale il picco di sinistra e dalla \n"
				+ "bisettrice verticale il picco superiore ";
		lista[5] = "Sono mostrati in verde i punti utilizzati per il fit della circonferenza";
		lista[6] = "La circonferenza risultante dal fit e' mostrata in rosso";
		lista[7] = "Il centro del fantoccio e' contrassegnato dal pallino rosso";
		lista[8] = "Si determina ora l'eventuale dimensione delle bolla d'aria, \n"
				+ "essa viene automaticamente compensata entro il limite del \n" + "\"maxBubbleGapLimit\"= ";
		lista[9] = "Viene mostrata la circonferenza con area 80% del fantoccio, chiamata MROI";
		lista[10] = "Il centro della MROI e' contrassegnato dal pallino verde";
		lista[11] = "11";
		lista[12] = "12";
		lista[13] = "13";
		lista[14] = "E'richiesto l'intervento manuale per il posizionamento di \n"
				+ "una ROI circolare di diametro corrispondente a quello esterno \n" + "del fantoccio";
		lista[15] = "Verifica posizionamento cerchio";
		lista[16] = "Troppa distanza tra i punti forniti ed il fit del cerchio";
		lista[17] = "Non si riescono a determinare le coordinate di almeno 3 punti del cerchio \n"
				+ "posizionare manualmente una ROI circolare di diametro uguale al fantoccio e\n" + "premere  OK";
		lista[18] = "Eventualmente spostare la MROI circolare di area pari all'80% del fantoccio";
		lista[19] = "19";
		lista[20] = "Viene ora esaltato il segnale sul fondo, al solo scopo di mostrare \n"
				+ "la ricerca del massimo segnale per i ghosts";
		lista[21] = "La circonferenza esterna del fantoccio, determinata \n" + "in precedenza e' mostrata in rosso";
		lista[22] = "Il centro del fantoccio e' contrassegnato dal pallino rosso";
		lista[23] = "Sono evidenziate le posizioni di massimo segnale medio, per \n" + "il calcolo dei ghosts";
		lista[24] = "Valore insolito di segnale del Ghost nella posizione selezionata, \n"
				+ "modificare eventualmente la posizione \n";
		lista[25] = "25";
		lista[26] = "26";
		lista[27] = "27";
		lista[28] = "28";
		lista[29] = "29";
		lista[30] = "Ricerca della posizione per il calcolo del segnale di fondo";
		lista[31] = "La circonferenza esterna del fantoccio, determinata in precedenza \n" + "e' mostrata in rosso";
		lista[32] = "Il centro del fantoccio e' contrassegnato dal pallino rosso";
		lista[33] = "Evidenziata la posizione per il calcolo del fondo";
		lista[34] = "Simulazione di ricerca posizione per calcolo del fondo non riuscita";
		lista[35] = "Roi per i ghost e fondo, sovrapposte alla immagine con fondo esaltato";
		lista[36] = "36";
		lista[37] = "37";
		lista[38] = "38";
		lista[39] = "39";
		// ---------+-----------------------------------------------------------+
		lista[40] = "Si calcolano le statistiche sull'area MROI, evidenziata in verde,\n"
				+ "il segnale medio vale S1= ";
		lista[41] = "Per valutare il noise, calcoliamo la immagine differenza tra \n"
				+ "le due immagini, l'immagine risultante e' costituita da rumore \n" + "piu' eventuali artefatti";
		lista[42] = "Il calcolo del rumore viene effettuato sulla immagine differenza, \n"
				+ "nell'area uguale a MROI, evidenziata in rosso, si indica con SD la \n"
				+ "Deviazione Standard di questa area SD1 = ";
		lista[43] = "Utilizzando la media del segnale sulla MROI evidenziata in verde \n"
				+ "sulla prima immagine e la deviazione standard di una identica roi evidenziata \n"
				+ "in rosso sulla immagine differenza, si calcola il rapporto Segnale/Rumore\n \n"
				+ "  snRatio = Math.sqrt(2) * S1 / SD1 \n \n  snRatio= ";
		lista[44] = "Viene generata una immagine a 5 sfumature di grigio, utilizzando \n"
				+ "come riferimento l'area evidenziata in rosso";
		lista[45] = "Questa e' l'immagine simulata, i gradini di colore evidenziano le \n" + "aree disuniformi";
		lista[46] = "Per il calcolo dell'Uniformita' percentuale si ricavano dalla MROI anche \n" + "i segnali ";
		lista[47] = "Da cui ottengo l'Uniformita' Integrale Percentuale\n \n"
				+ "  uiPerc = (1 - (max - min) / (max + min)) * 100 \n \n" + "  uiPerc = ";
		lista[48] = "48";
		lista[49] = "49";
		// ---------+-----------------------------------------------------------+
		lista[50] = "Per valutare i ghosts viene calcolata la statistica per ognuna delle 4 ROI, \n"
				+ "lo stesso per il fondo  \n \n  ghostPerc = ((mediaGhost - mediaFondo) / S1) * 100.0\n \n";
		lista[51] = "Spostamento automatico eccessivo per compensare la bolla \n"
				+ "d'aria presente nel fantoccio, verra' richiesto l'intervento \n" + "manuale";
		lista[52] = "52";
		lista[53] = "53";
		lista[54] = "54";
		lista[55] = "55";
		lista[56] = "56";
		lista[57] = "57";
		lista[58] = "58";
		lista[59] = "59";
		// ---------+-----------------------------------------------------------+
		lista[60] = "Eventualmente modificare la circonferenza con area 80% del \n" + "fantoccio, chiamata MROI";
		lista[61] = "61";
		lista[62] = "62";
		lista[63] = "63";
		lista[64] = "64";
		lista[65] = "vetMinimi==null, verificare esistenza della riga P12MIN nel file limiti.csv";
		lista[66] = "vetMaximi==null, verificare esistenza della riga P12MAX nel file limiti.csv";
		lista[67] = "67";
		lista[68] = "68";
		lista[69] = "69";
		// ---------+-----------------------------------------------------------+

		String out = lista[select];
		return out;
	}

	public static ResultsTable vectorResultsTable(int[] vetClassi) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		String t1 = "Numerosita' Classi";
		for (int i1 = 0; i1 < vetClassi.length; i1++) {
			rt1.incrementCounter();
			rt1.addValue(t1, vetClassi[i1]);
		}
		return rt1;
	}

	public static ResultsTable vectorResultsTable2(String[] vetVet) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		rt1.reset();
		for (int i1 = 0; i1 < vetVet.length; i1++) {
			rt1.incrementCounter();
			rt1.addValue("Numero", vetVet[i1]);
		}
		return rt1;
	}

	public static ResultsTable vectorResultsTable2(String[][] vetVet) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		rt1.reset();
		if (vetVet == null)
			MyLog.waitHere("vetVet==null");
		for (int i2 = 0; i2 < vetVet[0].length; i2++) {
			rt1.incrementCounter();
			for (int i1 = 0; i1 < vetVet.length; i1++) {
				rt1.addValue("Numero " + i1, vetVet[i1][i2]);
			}
		}
		return rt1;
	}

} // ultima
