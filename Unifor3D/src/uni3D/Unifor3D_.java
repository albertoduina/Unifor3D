package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import utils.ArrayUtils;
import utils.ImageUtils;
import utils.InputOutput;
import utils.MyCircleDetector;
import utils.MyConst;
import utils.MyLine;
import utils.MyLog;
import utils.MyStackUtils;
import utils.ReadDicom;
import utils.UtilAyv;
import ij.*;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.io.*;
import ij.plugin.*;
import ij.text.TextWindow;

//=====================================================
//     Programma per uniformita' 3D per immagini combined circolari
//     11 agosto 2016 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class Unifor3D_ implements PlugIn {
	static boolean debug = false;
	static int timeout = 200;

	public void run(String arg) {
		
		double minMean1 = +10;
		double maxMean1 = +4096;
		double minNoiseImaDiff = -2048;
		double maxNoiseImaDiff = +2048;
		double minSnRatio = +10;
		double maxSnRatio = +800;
		double minGhostPerc = -20;
		double maxGhostPerc = +20;
		double minUiPerc = +5;
		double maxUiPerc = +100;
		double minFitError = +0;
		double maxFitError = +20;
		double minBubbleGapLimit = 0;
		double maxBubbleGapLimit = 2;

	


		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}

		IJ.log("UNIFOR3D vi da'il benvenuto");

		// chiede di selezionare manualmente le cartelle con le
		// immagini Si suppone che le immagini siano trasferite utilizzando un
		// hard-disk OPPURE che venga fatto un DicomSorter.
		// QUINDI: IL PROGRAMMA SI FIDA DELL'OPERATORE

		DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA PRIMA ACQUISIZIONE");
		String dir1 = od1.getDirectory();
		String[] dir1a = new File(dir1).list();
		String[] dir1b = new String[dir1a.length];
		for (int i1 = 0; i1 < dir1a.length; i1++) {
			dir1b[i1] = dir1 + "\\" + dir1a[i1];
		}

		String[] sortedList1 = pathSorter(dir1b);
		ImagePlus imp10 = MyStackUtils.imagesToStack16(sortedList1);
		imp10.show();
		MyLog.waitHere();

		DirectoryChooser od2 = new DirectoryChooser("SELEZIONARE CARTELLA SECONDA ACQUISIZIONE");
		String dir2 = od2.getDirectory();
		String[] dir2a = new File(dir2).list();
		String[] dir2b = new String[dir2a.length];
		for (int i1 = 0; i1 < dir2a.length; i1++) {
			dir2b[i1] = dir2 + "\\" + dir2a[i1];
		}
		String[] sortedList2 = pathSorter(dir2b);
		ImagePlus imp20 = MyStackUtils.imagesToStack16(sortedList2);
		imp20.show();
		MyLog.waitHere();
		
		
		for (int i1=0; i1<sortedList1.length; i1++){

			// ===============================================
			ImagePlus imp11 = null;
				imp11 = UtilAyv.openImageMaximized(sortedList1[i1]);
				ImageUtils.imageToFront(imp11);
				if (imp11 == null)
				MyLog.waitHere("Non trovato il file " + sortedList1[i1]);
			ImagePlus imp13 = UtilAyv.openImageNoDisplay(sortedList2[i1], true);
			if (imp13 == null)
				MyLog.waitHere("Non trovato il file " + sortedList2[i1]);

			double out2[] = positionSearch11(imp11, maxFitError, maxBubbleGapLimit, info10, autoCalled, step, verbose,
					test, fast, timeout);
			if (out2 == null) {
				MyLog.waitHere("out2==null");
				return null;
			}

		
		
		

		IJ.showMessage("FINE LAVORO");
	} // chiude run

	// ############################################################################

	/***
	 * sort del vettore path in base a posizione immagine
	 * 
	 * @param path
	 * @return
	 */
	public static String[] pathSorter(String[] path) {
		if ((path == null) || (path.length == 0)) {
			IJ.log("pathSorter: path problems");
			return null;
		}
		Opener opener1 = new Opener();
		// test disponibilit� files
		for (int w1 = 0; w1 < path.length; w1++) {
			ImagePlus imp1 = opener1.openImage(path[w1]);
			if (imp1 == null) {
				IJ.log("pathSorter: image file unavailable: " + path[w1] + " ?");
				return null;
			}
		}
		String[] slicePosition = listSlicePosition(path);
		String[] pathSortato = bubbleSortPath(path, slicePosition);
		// new UtilAyv().logVector(pathSortato, "pathSortato");
		return pathSortato;
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
			boolean autoCalled, boolean step, boolean demo, boolean test, boolean fast, int timeout1) {

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
		if (demo) {
			iw11 = imp11.getWindow();
		}

		Overlay over12 = new Overlay();

		double dimPixel = ReadDicom.readDouble(
				ReadDicom.readSubstring(ReadDicom.readDicomParameter(imp11, MyConst.DICOM_PIXEL_SPACING), 1));

		MyCannyEdgeDetector mce = new MyCannyEdgeDetector();
		mce.setGaussianKernelRadius(2.0f);
		mce.setLowThreshold(2.5f);
		mce.setHighThreshold(10.0f);
		mce.setContrastNormalized(false);

		ImagePlus imp12 = mce.process(imp11);
		imp12.setOverlay(over12);
		MyLog.waitHere("output del CannyEdgeDetector 8 bit (solo 0 e 255) spessore 1 pixel");

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
			if (demo) {
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.updateAndDraw();
			}

			if (i1 == 1)
				vertical = true;
			else
				vertical = false;

			if (demo && i1 == 0)
				showProfiles = true;
			else
				showProfiles = false;

			myPeaks = cannyProfileAnalyzer(imp12, dimPixel, vetTitle[i1], showProfiles, demo, debug, vertical, timeout);

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
						ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[3][i2]), (int) (myPeaks[4][i2]), colore1,
								8.1);
						imp12.updateAndDraw();
						ImageUtils.imageToFront(imp12);
					}
					// MyLog.logVector(myXpoints, "myXpoints");
					// MyLog.logVector(myYpoints, "myYpoints");
				}
			}
		}
		if (demo)
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

		if (xPoints3.length < 3 || test) {
			UtilAyv.showImageMaximized(imp11);
			MyLog.waitHere(
					"Non si riescono a determinare le coordinate di almeno 3 punti del cerchio \n posizionare manualmente una ROI circolare di diametro uguale al fantoccio e\n premere  OK",
					debug, timeout1);
			manual = true;
		}

		if (!manual) {

			PointRoi pr12 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
			pr12.setPointType(2);
			pr12.setSize(4);
			imp12.setRoi(pr12);
			if (demo) {
				ImageUtils.addOverlayRoi(imp12, colore1, 8.1);
				pr12.setPointType(2);
				pr12.setSize(4);

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
			ImageUtils.fitCircle(imp12);
			if (demo) {
				imp12.getRoi().setStrokeColor(colore1);
				over12.addElement(imp12.getRoi());
			}

			if (demo)
				MyLog.waitHere("La circonferenza risultante dal fit e' mostrata in rosso", debug, timeout1);
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
				manual = true;
			}

			//
			// ----------------------------------------------------------
			// disegno la ROI del centro, a solo scopo dimostrativo !
			// ----------------------------------------------------------
			//

			if (demo) {
				MyCircleDetector.drawCenter(imp12, over12, xCenterCircle, yCenterCircle, colore1);
				MyLog.waitHere(listaMessaggi(7), debug, timeout1);

			}

			// =============================================================
			// COMPENSAZIONE PER EVENTUALE BOLLA D'ARIA NEL FANTOCCIO
			// ==============================================================

			// Traccio nuovamente le bisettrici verticale ed orizzontale, solo
			// che anziche' essere sul centro dell'immagine, ora sono poste sul
			// centro del cerchio circoscritto al fantoccio

			// BISETTRICE VERTICALE FANTOCCIO

			imp12.setRoi(new Line(xCenterCircle, 0, xCenterCircle, height));
			if (demo) {
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
			if (demo) {
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

			if (demo)
				MyLog.waitHere(listaMessaggi(8) + maxBubbleGapLimit, debug, timeout1);

			// Effettuo in ogni caso la correzione, solo che in assenza di bolla
			// d'aria la correzione sara' irrisoria, in presenza di bolla la
			// correzione sara' apprezzabile

			if (gapOrizz > gapVert) {
				xcorr = (int) gapOrizz / 2;
			} else {
				ycorr = (int) gapVert / 2;
			}

			// ---------------------------------------
			// qesto e' il risultato della nostra correzione e saranno i dati
			// della MROI
			diamMROI = (int) Math.round(diamCircle * MyConst.P3_AREA_PERC_80_DIAM);

			xCenterMROI = xCenterCircle + xcorr;
			yCenterMROI = yCenterCircle + ycorr;
			// ---------------------------------------
			// verifico ora che l'entita' della bolla non sia cosi' grande da
			// portare l'area MROI troppo a contatto del profilo fantoccio
			// calcolato sulle bisettrici
			// ---------------------------------------
			imp12.setRoi(new Line(xCenterMROI, 0, xCenterMROI, height));
			if (demo) {
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
			if (demo) {
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

				MyLog.waitHere(listaMessaggi(51) + " dMin= " + dMin, debug, timeout1);
			} else {
				imp12.setRoi(new OvalRoi(xCenterMROI - diamMROI / 2, yCenterMROI - diamMROI / 2, diamMROI, diamMROI));
				Rectangle boundingRectangle2 = imp12.getProcessor().getRoi();
				diamMROI = (int) boundingRectangle2.width;
				xCenterMROI = boundingRectangle2.x + boundingRectangle2.width / 2;
				yCenterMROI = boundingRectangle2.y + boundingRectangle2.height / 2;
				// imp12.killRoi();
			}
		}

		if (manual) {
			// ==================================================================
			// INTERVENTO MANUALE PER CASI DISPERATI, SAN GENNARO PENSACI TU
			// ==================================================================
			fast = false;
			imp12.close();
			over12.clear();
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

		if (demo) {
			imp12.updateAndDraw();
			imp12.getRoi().setStrokeColor(colore2);
			over12.addElement(imp12.getRoi());
			MyLog.waitHere(listaMessaggi(9), debug, timeout1);
			MyCircleDetector.drawCenter(imp12, over12, xCenterCircle + xcorr, yCenterCircle + ycorr, colore2);
			MyLog.waitHere(listaMessaggi(10), debug, timeout1);
		}

		imp12.close();
		double[] out2 = new double[6];
		out2[0] = xCenterCircle;
		out2[1] = yCenterCircle;
		out2[2] = diamCircle;
		out2[3] = xCenterMROI;
		out2[4] = yCenterMROI;
		out2[5] = diamMROI;
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

} // ultima
