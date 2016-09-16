package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.DialogListener;
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
import ij.io.FileSaver;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import utils.ArrayUtils;
import utils.ImageUtils;
import utils.InputOutput;
import utils.AboutBox;
import utils.MyCircleDetector;
import utils.MyConst;
import utils.MyFilter;
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
//     Programma per plot 3D per immagini uncombined circolari
//     1 settembre 2016 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class Uncombined3D_MAPPAZZAold implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = false;
	public static String VERSION = "CDQ 3D";

	public void run(String arg) {

		new AboutBox().about("Uncombined3D", MyVersion.CURRENT_VERSION);
		IJ.wait(20);
		new AboutBox().close();

		GenericDialog gd = new GenericDialog("", IJ.getInstance());
		String[] items = { "5 livelli", "12 livelli" };
		gd.addRadioButtonGroup("SIMULATE", items, 2, 2, "5 livelli");
		gd.addCheckbox("auto", true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		String level = gd.getNextRadioButton();
		boolean auto = gd.getNextBoolean();
		boolean twelve;
		if (level.equals("5 livelli")) {
			twelve = false;
		} else {
			twelve = true;
		}
		ArrayList<Integer> pixListSignal11 = new ArrayList<Integer>();

		IJ.log("-----IW2AYV----");
		UtilAyv.logResizer(200, 200, 400, 400);

		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}
		ImagePlus imp10 = null;
		String myName = null;
		String path10 = null;
		String[] dir1a = null;
		String dir10 = null;
		String dir1 = null;
		int num = 0;
		// if (auto) {
		// dir10 = Prefs.get("prefer.Unifor3D_dir3", "none");
		// DirectoryChooser.setDefaultDirectory(dir10);
		// DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA
		// STACK");
		// dir1 = od1.getDirectory();
		// Prefs.set("prefer.Unifor3D_dir3", dir1);
		// dir1a = new File(dir1).list();
		// num = dir1a.length;
		// } else {
		// path10 = UtilAyv.imageSelection("SELEZIONARE LO STACK DA ELABORARE");
		// num = 1;
		// }
		//
		int count0 = -1;
		boolean loop1 = true;
		ImagePlus impMappazza = null;
		boolean debug1 = false;

		while (loop1) {

			path10 = UtilAyv.imageSelection("SELEZIONARE LO STACK DA ELABORARE");
			if (path10 == null) {
				loop1 = false;
				continue;
			}

			// MyLog.waitHere("path10= " + path10);
			imp10 = UtilAyv.openImageNormal(path10);
			myName = imp10.getTitle();

			count0++;
			int width = imp10.getWidth();
			int height = imp10.getHeight();
			if (impMappazza == null) {
				impMappazza = generaMappazzaVuota(width, height, imp10.getImageStackSize());
			}

			ImageStack imaStack = imp10.getImageStack();
			if (imaStack == null) {
				IJ.log("imageFromStack.imaStack== null");
				return;
			}

			if (imaStack.getSize() < 2) {
				MyLog.waitHere("Per le elaborazioni 3D ci vuole uno stack di più immagini!");
				return;
			}

			IJ.run(imp10, "Orthogonal Views", "");
			Orthogonal_Views ort1 = Orthogonal_Views.getInstance();
			if (step)
				MyLog.waitHere("output di 'Orthogonal Views'");

			ImagePlus imp102 = ort1.getXZImage();
			if (imp102 == null)
				MyLog.waitHere("imp102=null");
			IJ.wait(100);
			ImagePlus imp202 = new Duplicator().run(imp102);
			IJ.wait(10);

			ImagePlus imp103 = ort1.getYZImage();
			if (imp103 == null)
				MyLog.waitHere("imp103=null");
			// ImagePlus imp203 = imp103.duplicate();
			ImagePlus imp203 = new Duplicator().run(imp103);
			IJ.wait(10);
			imp202.show();
			imp203.show();

			int mode = 3;
			if (auto)
				mode = 0;
			double[] out202 = positionSearchPhantom(imp202, mode, timeout);
			Overlay over202 = new Overlay();
			imp202.setOverlay(over202);
			double xCenterEXT = out202[0];
			double yCenterEXT = out202[1];
			double diamEXT = out202[2];
			imp202.setRoi(new OvalRoi(xCenterEXT - diamEXT / 2, yCenterEXT - diamEXT / 2, diamEXT, diamEXT));
			imp202.getRoi().setStrokeColor(Color.green);
			over202.addElement(imp202.getRoi());
			imp202.deleteRoi();
			imp202.updateAndDraw();

			double[] out203 = positionSearchPhantom(imp203, mode, timeout);
			Overlay over203 = new Overlay();
			imp203.setOverlay(over203);
			xCenterEXT = out203[0];
			yCenterEXT = out203[1];
			diamEXT = out203[2];
			imp203.setRoi(new OvalRoi(xCenterEXT - diamEXT / 2, yCenterEXT - diamEXT / 2, diamEXT, diamEXT));
			imp203.getRoi().setStrokeColor(Color.green);
			over203.addElement(imp203.getRoi());
			imp203.deleteRoi();
			imp203.updateAndDraw();

			// ===============================
			// IMMAGINE DI CENTRO DELLA SFERA
			// ===============================
			int centerSlice = 0;
			if ((out202[1] - out203[0]) < 2 || (out203[0] - out202[1]) < 2) {
				centerSlice = (int) out202[1];
			}

			if (centerSlice == 0)
				centerSlice = imaStack.getSize() / 2;

			ImagePlus imp101 = MyStackUtils.imageFromStack(imp10, centerSlice);
			if (imp101 == null)
				MyLog.waitHere("imp101=null");
			ImagePlus imp201 = imp101.duplicate();
			double[] out201 = positionSearchPhantom(imp201, mode, timeout);
			imp201.show();
			Overlay over201 = new Overlay();
			imp201.setOverlay(over201);
			xCenterEXT = out201[0];
			yCenterEXT = out201[1];
			diamEXT = out201[2];
			imp201.setRoi(new OvalRoi(xCenterEXT - diamEXT / 2, yCenterEXT - diamEXT / 2, diamEXT, diamEXT));
			imp201.getRoi().setStrokeColor(Color.red);
			over201.addElement(imp201.getRoi());
			imp201.deleteRoi();
			imp201.updateAndDraw();
			IJ.run("Tile", "");

			// MyLog.waitHere("endPositionSearchPhantom");

			// al contrario dell'uniformita' non mi baso su una
			// ricostruzione
			// geometrica dela sfera ma effettuo la ricerca dello spot11x11
			// su
			// tutte
			// le sezioni
			double profond = 30;
			mode = 0;
			ImageStack newStack = new ImageStack(width, height);

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {
				if (!auto)
					IJ.log("localizzo hotspot " + i1 + " / " + imp10.getImageStackSize());
				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				double[] pos20 = positionSearchZZ(imp20, profond, "", mode, timeout);
				if (pos20 == null) {
					continue;
				}
				double diamMROI = 11;
				double xCenterRoi = pos20[0];
				double yCenterRoi = pos20[1];
				ImagePlus imp21 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				pixVectorize(imp21, xCenterRoi, yCenterRoi, diamMROI, pixListSignal11);
				// IJ.wait(timeout);
				imp20.close();
				imp21.close();
			}
			int[] pixListSignal = ArrayUtils.arrayListToArrayInt(pixListSignal11);
			double mean11 = UtilAyv.vetMean(pixListSignal);

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {
				if (!auto)
					IJ.log("calcolo mappazza " + i1 + " / " + imp10.getImageStackSize());
				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				mappazza5Colori(mean11, imp20, impMappazza, i1 + 1, count0, debug1);
			}

			impMappazza.show();
			impMappazza.updateAndRepaintWindow();
			debug1 = true;

			MyLog.waitHere();

			// if (auto) {
			// Path path100 = Paths.get(dir10);
			// Path path101 = path100.getParent();
			//
			// String lev = null;
			// if (twelve)
			// lev = "12_livelli";
			// else
			// lev = "5_livelli";
			// boolean ok1 = createDirectory(path101 + "\\simul_" + lev + "\\");
			// String aux1 = path101 + "\\simul_" + lev + "\\" + myName + "sim";
			// // MyLog.waitHere("aux1= " + aux1);
			// new FileSaver(mappazzaStack).saveAsTiff(aux1);
			//
			// UtilAyv.cleanUp();
			// }
			//
			// int num1 = 0;
			//
			// String lev = null;
			// if (twelve) {
			// lev = "12_livelli";
			// num1 = 12;
			// } else {
			// lev = "5_livelli";
			// num1 = 5;
			// }
			// Path path100 = Paths.get(dir10);
			// Path path101 = path100.getParent();
			// ImagePlus scala = ImageUtils.generaScalaColori(num1);
			// String aux2 = path101 + "\\simul_" + lev + "\\" + myName +
			// "scala";
			// // MyLog.waitHere("aux1= " + aux1);
			// new FileSaver(scala).saveAsTiff(aux2);
			// UtilAyv.cleanUp();

		}
	} // chiude

	/**
	 * Creazione di una directory
	 * 
	 * @param directoryPath
	 *            path da creare
	 * @return true se ok
	 */
	public static boolean createDirectory(String directoryPath) {
		boolean exists = (new File(directoryPath)).exists();
		if (!exists) {
			exists = (new File(directoryPath)).mkdirs();
			if (!exists) {
				IJ.error("fallita la creazione di " + directoryPath);
			}
		}
		return (exists);
	}

	/**
	 * Calcola le coordinate del centro ROI sul segmento circonferenza - centro,
	 * alla profondita' desiderata
	 * 
	 * @param ax
	 *            coordinata X su circonferenza
	 * @param ay
	 *            coordinata Y su circonferenza
	 * @param bx
	 *            coordinata X del centro
	 * @param by
	 *            coordinata Y del centro
	 * @param prof
	 *            profondita' centro ROI
	 * @return vettore coordinate centro ROI
	 */
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

	/**
	 * Dati i punti di inizio e fine di un segmento, restituisce il valore
	 * dell'angolo theta, effettuando la conversione da coordinate rettangolari
	 * (x,y) a coordinate polari (r, theta). NB: tiene conto che in ImageJ la
	 * coordinata Y ha lo 0 in alto a sx, anziche' in basso a sx, come siamo
	 * soliti a vedere il piano cartesiano
	 * 
	 * @param ax
	 *            coordinata X inizio
	 * @param ay
	 *            coordinata Y inizio
	 * @param bx
	 *            coordinata X fine
	 * @param by
	 *            coordinata Y fine
	 * @return valore dell'angolo in radianti
	 */
	public static double angoloRad(double ax, double ay, double bx, double by) {

		double dx = ax - bx;
		double dy = by - ay; // dy e' all'incontrario, per le coordinate di
								// ImageJ
		double theta = Math.atan2(dy, dx);
		return theta;

	}

	/**
	 * Riceve una ImagePlus con impostata una Line, restituisce le coordinate
	 * dei 2 picchi. Se i picchi non sono 1 oppure 2, restituisce null.
	 * 
	 * @param imp1
	 * @param dimPixel
	 * @param title
	 * @param showProfiles
	 * @return
	 */
	public static double[][] profileAnalyzer(ImagePlus imp1, double dimPixel, String title, boolean showProfiles,
			boolean vertical, int timeout) {

		// MyLog.waitHere("showProfiles= " + showProfiles);

		double[][] profi3 = MyLine.decomposer(imp1);

		double[] vetz = new double[profi3[0].length];
		for (int i1 = 0; i1 < profi3[0].length; i1++) {
			vetz[i1] = profi3[2][i1];
		}
		// double[] minmax = Tools.getMinMax(vetz);

		ArrayList<ArrayList<Double>> matOut = null;
		double[][] peaks1 = null;

		// double limit = minmax[1] / 20;
		// if (limit < 100)
		// limit = 100;
		double limit = 100;

		do {
			matOut = ImageUtils.peakDet1(profi3, limit);
			peaks1 = new InputOutput().fromArrayListToDoubleTable(matOut);
			if (peaks1 == null) {
				// MyLog.waitHere("peaks1 == null");
				return null;
			}

			if (peaks1.length == 0) {
				// MyLog.waitHere("peaks1.length == 0");
				return null;
			}
			if (peaks1[0].length == 0) {
				// MyLog.waitHere("peaks1[0].length == 0");
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

			MyLog.waitHere(listaMessaggi(5), debug, timeout);

		}

		if (WindowManager.getFrame(title) != null) {
			IJ.selectWindow(title);
			IJ.run("Close");
		}

		return peaks1;
	}

	public static String[] estrai(String[][] list1, int coil1, int num1) {

		String[] list2 = new String[num1];
		int aux1 = coil1 * num1;
		for (int w1 = 0; w1 < num1; w1++) {
			list2[w1] = list1[0][aux1 + w1];
		}
		return list2;
	}

	public static void pixVectorize(ImagePlus imp11, double xCenterMROI, double yCenterMROI, double diamMROI,
			ArrayList<Integer> pixList11) {

		if (imp11 == null)
			MyLog.waitHere("imp11==null");
		if (pixList11 == null)
			MyLog.waitHere("pixList1==null");

		imp11.setRoi((int) Math.round((xCenterMROI - 5.5)), (int) Math.round(yCenterMROI - 5.5), 11, 11);
		Roi roi11 = imp11.getRoi();
		ImageProcessor ip11 = imp11.getProcessor();
		if (ip11 == null)
			MyLog.waitHere("ip11==null");
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

	public static ResultsTable vectorResultsTable2(double[] vetVet) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		rt1.reset();
		for (int i1 = 0; i1 < vetVet.length; i1++) {
			rt1.incrementCounter();
			rt1.addValue("Numero", vetVet[i1]);
		}
		return rt1;
	}

	public static ResultsTable vectorResultsTable2(String[] vetKey, String[] vetVet) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		rt1.reset();
		for (int i1 = 0; i1 < vetVet.length; i1++) {
			rt1.incrementCounter();
			rt1.addValue("Key", vetKey[i1]);
			rt1.addValue("Numero", vetVet[i1]);
		}
		return rt1;
	}

	public static ResultsTable vectorResultsTable2(double[] vetKey, int[] vetVet) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		rt1.reset();
		for (int i1 = 0; i1 < vetVet.length; i1++) {
			rt1.incrementCounter();
			rt1.addValue("Key", vetKey[i1]);
			rt1.addValue("Numero", vetVet[i1]);
		}
		return rt1;
	}

	public static ResultsTable vectorResultsTable2(String[] vetKey, int[] vetVet) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		rt1.reset();
		for (int i1 = 0; i1 < vetVet.length; i1++) {
			rt1.incrementCounter();
			rt1.addValue("Key", vetKey[i1]);
			rt1.addValue("Numero", vetVet[i1]);
		}
		return rt1;
	}

	public static ResultsTable vectorResultsTable2(int[] vetVet) {

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

	/**
	 * Ricerca della posizione della ROI per il calcolo dell'uniformita'
	 * 
	 * @param imp11
	 *            immagine di input
	 * @param profond
	 *            profondita' ROI
	 * @param info1
	 *            messaggio esplicativo
	 * @param autoCalled
	 *            flag true se chiamato in automatico
	 * @param step
	 *            flag true se funzionamento passo - passo
	 * @param verbose
	 *            flag true se funzionamento verbose
	 * @param test
	 *            flag true se in test
	 * @param fast
	 *            flag true se modo batch
	 * @return vettore con dati ROI
	 */

	public static double[] positionSearchZZ(ImagePlus imp11, double profond, String info1, int mode, int timeout) {
		// boolean autoCalled=false;

		boolean demo = false;
		if (mode == 10 || mode == 3) {
			demo = true;
		}

		// boolean demo = verbose;
		// boolean showProfiles = demo;

		if (imp11 == null)
			MyLog.waitHere("imp11==null");

		if (demo)
			imp11.show();
		ImageWindow iw11 = null;
		if (demo)
			iw11 = imp11.getWindow();

		double[] out10 = MyFilter.maxPosition11x11_NEW(imp11);
		if (out10 == null) {
			if (iw11 == null) {
			} else
				iw11.close();
			imp11.close();
			return null;
		}
		int latoMaxima = 11;
		int xMaxima = (int) Math.round(out10[0] - latoMaxima / 2);
		int yMaxima = (int) Math.round(out10[1] - latoMaxima / 2);

		imp11.setRoi(xMaxima, yMaxima, latoMaxima, latoMaxima);

		imp11.updateAndDraw();
		if (demo)
			IJ.wait(timeout);
		if (iw11 == null) {
		} else
			iw11.close();
		imp11.close();

		return out10;
	}

	public static double[] positionSearchPhantom(ImagePlus imp11, int mode, int timeout) {

		// boolean autoCalled=false;

		boolean demo = false;
		Color colore1 = Color.red;
		Color colore2 = Color.green;
		Color colore3 = Color.red;

		if (mode == 10 || mode == 3)
			demo = true;

		boolean manual = false;
		int xCenterCircle = 0;
		int yCenterCircle = 0;
		int diamCircle = 0;

		double maxFitError = 30;
		if (imp11 == null)
			MyLog.waitHere("imp11==null");

		ImageWindow iw11 = null;
		if (demo)
			iw11 = imp11.getWindow();

		int width = imp11.getWidth();
		int height = imp11.getHeight();

		ImagePlus imp12 = imp11.duplicate();
		imp12.setTitle("DUPLICATO di imp11");
		Overlay over12 = new Overlay();
		imp12.setOverlay(over12);

		// -------------------------------------------------
		// Determinazione del cerchio
		// -------------------------------------------------
		//
		// IJ.run(imp12, "Smooth", "");

		ImageProcessor ip12 = imp12.getProcessor();
		if (demo) {
			imp12.show();
			ImageUtils.imageToFront(imp12);
			MyLog.waitHere("L'immagine verra' processata con il filtro variance, per estrarre il bordo", debug,
					timeout);
		}

		// ip12.findEdges();
		RankFilters rk1 = new RankFilters();
		double radius = 0.1;
		int filterType = RankFilters.VARIANCE;
		rk1.rank(ip12, radius, filterType);
		imp12.updateAndDraw();
		if (demo)
			MyLog.waitHere("L'immagine risultante ha il bordo con il segnale fortemente evidenziato", debug, timeout);

		// =============== modifica 290515 ===========
		double max1 = imp12.getStatistics().max;
		ip12.subtract(max1 / 30);
		// ===========================================

		imp12.updateAndDraw();
		if (demo)
			MyLog.waitHere(
					"All'intera immagine viene sottratto 1/30 del segnale massimo,\n questo per eliminare eventuale noise residuo",
					debug, timeout);

		// if (demo)
		// MyLog.waitHere(listaMessaggi(3), debug, timeout);

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

			double dimPixel = 1;
			myPeaks = profileAnalyzer(imp12, dimPixel, vetTitle[i1], showProfiles, vertical, timeout);
			if (myPeaks == null) {
			} else {
				for (int i2 = 0; i2 < myPeaks[0].length; i2++) {
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

		if (demo)
			MyLog.waitHere("Si tracciano ulteriori linee ", debug, timeout);

		if (count >= 0)

		{
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
		over12.clear();
		imp12.deleteRoi();
		imp12.updateAndDraw();

		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------
		imp11.show();
		if (xPoints3 == null || xPoints3.length < 3) {
			imp11.show();
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
			// if (!manualOverride)
			// writeStoredRoiData(boundRec);

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
				// IJ.log("debug at i1= " + i1 + " sumError= " + sumError);
			}
			if (sumError > maxFitError) {
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
				imp12.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.deleteRoi();
				// MyLog.logVector(xPoints3, "xPoints3");
				// MyLog.logVector(yPoints3, "yPoints3");
				MyLog.waitHere(listaMessaggi(18) + " erano " + xPoints3.length + " punti", debug);
				manual = true;
			}

		}

		// MyLog.waitHere("manual= " + manual);
		// MyLog.waitHere("xPoints3.length= " + xPoints3.length);

		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------
		// if (xPoints3 != null && xPoints3.length >= 3 && !manual)
		// {
		// // MyLog.waitHere("AUTO");
		// imp12.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
		// ImageUtils.fitCircle(imp12);
		// if (demo) {
		// over12.addElement(imp12.getRoi());
		// over12.setStrokeColor(Color.cyan);
		// }
		// }
		// imp12.deleteRoi();

		over12.clear();
		imp12.close();

		double[] out2 = new double[3];
		out2[0] = xCenterCircle;
		out2[1] = yCenterCircle;
		out2[2] = diamCircle;
		return out2;
	}

	/**
	 * Ricerca della posizione della ROI per il calcolo dell'uniformita'
	 * 
	 * @param imp11
	 *            immagine di input
	 * @param profond
	 *            profondita' ROI
	 * @param info1
	 *            messaggio esplicativo
	 * @param autoCalled
	 *            flag true se chiamato in automatico
	 * @param step
	 *            flag true se funzionamento passo - passo
	 * @param verbose
	 *            flag true se funzionamento verbose
	 * @param test
	 *            flag true se in test
	 * @param fast
	 *            flag true se modo batch
	 * @return vettore con dati ROI
	 */

	public static double[] positionSearchZZOLD(ImagePlus imp11, double profond, String info1, int mode, int timeout) {
		// boolean autoCalled=false;

		boolean demo = false;
		Color colore1 = Color.red;
		Color colore2 = Color.green;
		Color colore3 = Color.red;

		if (mode == 10 || mode == 3) {
			demo = true;
		}
		// boolean step = false;
		// boolean verbose = false;
		// boolean test = false;
		// boolean fast = false;
		//

		// ================================================================================
		// Inizio calcoli geometrici
		// ================================================================================
		//

		// MyLog.waitHere("autoCalled= " + autoCalled + "\nstep= " + step
		// + "\nverbose= " + verbose + "\ntest= " + test + "\nfast= "
		// + fast);

		boolean manual = false;
		// boolean demo = verbose;
		// boolean showProfiles = demo;
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

		// double dimPixel = ReadDicom.readDouble(
		// ReadDicom.readSubstring(ReadDicom.readDicomParameter(imp11,
		// MyConst.DICOM_PIXEL_SPACING), 1));

		ImageWindow iw11 = null;
		if (demo)
			iw11 = imp11.getWindow();

		int width = imp11.getWidth();
		int height = imp11.getHeight();
		ImagePlus imp12 = imp11.duplicate();
		imp12.setTitle("DUP");

		// ************************************
		// UtilAyv.showImageMaximized(imp12);
		// UtilAyv.showImageMaximized(imp11);
		// ************************************

		//
		// -------------------------------------------------
		// Determinazione del cerchio
		// -------------------------------------------------
		//
		// IJ.run(imp12, "Smooth", "");

		ImageProcessor ip12 = imp12.getProcessor();
		if (demo) {
			imp12.show();
			ImageUtils.imageToFront(imp12);
			MyLog.waitHere("L'immagine verra' processata con il filtro variance, per estrarre il bordo", debug,
					timeout);
		}

		// ip12.findEdges();
		RankFilters rk1 = new RankFilters();

		double radius = 0.1;
		int filterType = RankFilters.VARIANCE;
		rk1.rank(ip12, radius, filterType);
		if (demo)
			imp12.updateAndDraw();
		if (demo)
			MyLog.waitHere("L'immagine risultante ha il bordo con il segnale fortemente evidenziato", debug, timeout);

		// =============== modifica 290515 ===========
		double max1 = imp12.getStatistics().max;
		ip12.subtract(max1 / 30);
		// ===========================================

		if (demo)
			imp12.updateAndDraw();
		if (demo)
			MyLog.waitHere(
					"All'intera immagine viene sottratto 1/30 del segnale massimo,\n questo per eliminare eventuale noise residuo",
					debug, timeout);

		// if (demo)
		// MyLog.waitHere(listaMessaggi(3), debug, timeout);

		if (demo)
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

			// if (demo && i1 == 0)
			// showProfiles = true;

			double dimPixel = 1;
			myPeaks = profileAnalyzer(imp12, dimPixel, vetTitle[i1], showProfiles, vertical, timeout);
			if (myPeaks == null) {
			} else {
				for (int i2 = 0; i2 < myPeaks[0].length; i2++) {
					count++;
					myXpoints[count] = (int) (myPeaks[0][i2]);
					myYpoints[count] = (int) (myPeaks[1][i2]);
					ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[0][i2]), (int) (myPeaks[1][i2]), colore1);
					imp12.updateAndDraw();
					ImageUtils.imageToFront(imp12);
				}
			}
		}

		// NON CAPISCO PIU' A CHE SERVIVA

		// devo compattare i vettori myXpoints e myYpoints perchè i punti utili
		// possono essere meno di 16
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
		if (demo)
			imp12.updateAndDraw();

		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------
		if (demo)
			imp11.show();
		if (xPoints3 == null || xPoints3.length < 3) {

			over12.clear();
			imp11.close();
			imp12.close();

			// MyLog.waitHere("non riesco a determinare il profilo fantoccio");
			return null;
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
			// if (!manualOverride)
			// writeStoredRoiData(boundRec);
			double minDiamPhantom = 80;
			if (diamCircle < minDiamPhantom) {
				// MyLog.waitHere("diametro fantoccio troppoo piccolo");
				over12.clear();
				imp11.close();
				imp12.close();

				return null;
			}

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
				// IJ.log("debug at i1= " + i1 + " sumError= " + sumError);
			}
			if (sumError > maxFitError) {
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
				imp12.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
				imp12.getRoi().setStrokeColor(colore2);
				over12.addElement(imp12.getRoi());
				imp12.deleteRoi();
				// MyLog.logVector(xPoints3, "xPoints3");
				// MyLog.logVector(yPoints3, "yPoints3");
				MyLog.waitHere(listaMessaggi(18) + " erano " + xPoints3.length + " punti", debug);
				manual = true;
			}

		}

		// MyLog.waitHere("manual= " + manual);
		// MyLog.waitHere("xPoints3.length= " + xPoints3.length);

		// ----------------------------------------------------------------------
		// Verifica di avere trovato almeno 3 punti, altrimenti chiede la
		// selezione manuale del cerchio
		// -------------------------------------------------------------------
		if (xPoints3 != null && xPoints3.length >= 3 && !manual)

		{
			// MyLog.waitHere("AUTO");
			imp12.setRoi(new PointRoi(xPoints3, yPoints3, xPoints3.length));
			ImageUtils.fitCircle(imp12);
			if (demo) {
				over12.addElement(imp12.getRoi());
				over12.setStrokeColor(Color.red);
			}

		} else

		{

			// NON SI SONO DETERMINATI 3 PUNTI DEL CERCHIO, SELEZIONE MANUALE
			Rectangle boundRec1 = null;
			Rectangle boundRec2 = null;

			if (!imp11.isVisible())
				UtilAyv.showImageMaximized(imp11);
			// UtilAyv.showImageMaximized(imp11);
			// ImageUtils.imageToFront(iw11);
			imp11.setRoi(new OvalRoi((width / 2) - 100, (height / 2) - 100, 200, 200));
			imp11.updateAndDraw();
			boundRec1 = imp11.getProcessor().getRoi();

			MyLog.waitHere(listaMessaggi(19), debug, timeout);

			// OBBLIGO A CAMBIARE QUALCOSA PER PREVENIRE L'OK "SCIMMIA"
			if (timeout > 0) {
				IJ.wait(100);
				imp11.setRoi(new OvalRoi((width / 2) - 101, (height / 2) - 100, 200, 200));
			}

			boundRec2 = imp11.getProcessor().getRoi();

			while (boundRec1.equals(boundRec2)) {
				MyLog.waitHere(listaMessaggi(40), debug);
				boundRec2 = imp11.getProcessor().getRoi();
			}

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

		Rectangle boundRec = null;
		if (manual)
			boundRec = imp11.getProcessor().getRoi();
		else
			boundRec = imp12.getProcessor().getRoi();

		imp12.close();
		// x1 ed y1 sono le due coordinate del centro

		xCenterCircle = boundRec.x + boundRec.width / 2;
		yCenterCircle = boundRec.y + boundRec.height / 2;
		diamCircle = boundRec.width;
		MyCircleDetector.drawCenter(imp11, over12, xCenterCircle, yCenterCircle, Color.red);

		// ----------------------------------------------------------
		// disegno la ROI del maxima, a solo scopo dimostrativo !
		// ----------------------------------------------------------
		//

		// x1 ed y1 sono le due coordinate del punto di maxima

		// double[] out10 = UtilAyv.findMaximumPosition(imp12);

		double[] out10 = MyFilter.maxPosition11x11_NEW(imp11);
		xMaxima = out10[0];
		yMaxima = out10[1];
		MyCircleDetector.drawCenter(imp11, over12, xMaxima, yMaxima, Color.green);
		// MyLog.waitHere();
		// over12.clear();

		if (demo)

		{
			MyCircleDetector.drawCenter(imp11, over12, xMaxima, yMaxima, Color.green);

			if (demo)
				MyLog.waitHere(listaMessaggi(20), debug, timeout);

		}
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
		if (lun1 < lun2)

		{
			xBordo = out11[0];
			yBordo = out11[1];
		} else

		{
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

		double dimPixel = 1;
		double[] out1 = interpolaProfondCentroROI(xEndRefLine, yEndRefLine, xStartRefLine, yStartRefLine,
				profond / dimPixel);
		ax = out1[0];
		ay = out1[1];

		if (demo)

		{
			MyCircleDetector.drawCenter(imp11, over12, (int) ax, (int) ay, Color.yellow);
			MyLog.waitHere(listaMessaggi(21), debug, timeout);
		}

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

		if (demo && manual)

		{

			ImageUtils.imageToFront(iw11);

			// UtilAyv.showImageMaximized(imp11);
			imp11.setOverlay(over12);
			imp11.setRoi((int) ax - sqNEA / 2, (int) ay - sqNEA / 2, sqNEA, sqNEA);
			imp11.updateAndDraw();
			if (demo)
				MyLog.waitHere(info1 + "\n \nMODIFICA MANUALE POSIZIONE ROI", debug, timeout);
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

	public static ImagePlus simulata5Colori(double mean11, ImagePlus imp1) {

		if (imp1 == null) {
			IJ.error("Simula5Colori ricevuto null");
			return (null);
		}
		int width = imp1.getWidth();
		int height = imp1.getHeight();
		short[] pixels1 = UtilAyv.truePixels(imp1);

		double mean = mean11;
		double minus20 = mean * MyConst.MINUS_20_PERC;
		double minus10 = mean * MyConst.MINUS_10_PERC;
		double plus10 = mean * MyConst.PLUS_10_PERC;
		double plus20 = mean * MyConst.PLUS_20_PERC;
		// genero una immagine nera

		ImageProcessor ipSimulata = new ColorProcessor(width, height);
		int[] pixelsSimulata = (int[]) ipSimulata.getPixels();

		short pixSorgente = 0;
		int pixSimulata = 0;
		int posizioneArrayImmagine = 0;

		int colorP20 = ((255 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
		int colorP10 = ((255 & 0xff) << 16) | ((165 & 0xff) << 8) | (0 & 0xff);
		int colorMED = ((255 & 0xff) << 16) | ((255 & 0xff) << 8) | (0 & 0xff);
		int colorM10 = ((124 & 0xff) << 16) | ((252 & 0xff) << 8) | (50 & 0xff);
		int colorM20 = ((0 & 0xff) << 16) | ((128 & 0xff) << 8) | (0 & 0xff);

		int colorOUT = ((0 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff); // nero

		for (int y = 0; y < width; y++) {
			for (int x = 0; x < width; x++) {
				posizioneArrayImmagine = y * width + x;
				pixSorgente = pixels1[posizioneArrayImmagine];
				if (pixSorgente > plus20)
					pixSimulata = colorP20;
				else if (pixSorgente > plus10)
					pixSimulata = colorP10;
				else if (pixSorgente > minus10)
					pixSimulata = colorMED;
				else if (pixSorgente > minus20)
					pixSimulata = colorM10;
				else if (pixSorgente > 100)
					pixSimulata = colorM20;
				else
					pixSimulata = colorOUT;
				pixelsSimulata[posizioneArrayImmagine] = pixSimulata;
			}
		}

		ipSimulata.resetMinAndMax();
		ImagePlus impSimulata = new ImagePlus("ColorSimulata", ipSimulata);

		return impSimulata;
	}

	public static ImagePlus generaMappazzaVuota(int width, int height, int depth) {

		int bitdepth = 24;
		ImageStack newStack = ImageStack.create(width, height, depth, bitdepth);
		ImagePlus impMappazza = new ImagePlus("MAPPAZZA", newStack);
		return impMappazza;
	}

	public static void mappazza5ColoriOLD(double mean11, ImagePlus imp1, ImagePlus impMappazza, int slice) {

		if (imp1 == null) {
			MyLog.waitHere("imp1==null");
			return;
		}
		if (impMappazza == null) {
			MyLog.waitHere("impMappazza==null");
			return;
		}

		int width = imp1.getWidth();
		int height = imp1.getHeight();
		short[] pixels1 = UtilAyv.truePixels(imp1);
		double mean = mean11 * 0.8;
		double minus20 = mean * MyConst.MINUS_20_PERC;
		double minus10 = mean * MyConst.MINUS_10_PERC;
		double plus10 = mean * MyConst.PLUS_10_PERC;
		double plus20 = mean * MyConst.PLUS_20_PERC;
		int colorOUT = 0;

		ImageStack stack1 = impMappazza.getStack();
		ImageProcessor ipMappa = stack1.getProcessor(slice);
		int[] pixelsMappa = (int[]) ipMappa.getPixels();

		// ImageProcessor ipSimulata = new ColorProcessor(width, height);
		// int[] pixelsSimulata = (int[]) ipSimulata.getPixels();

		short pixSorgente = 0;
		int aux1 = 0;
		int posizioneArrayImmagine = 0;
		int colorP20 = ((255 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
		int colorP10 = ((255 & 0xff) << 16) | ((165 & 0xff) << 8) | (0 & 0xff);
		int colorMED = ((255 & 0xff) << 16) | ((255 & 0xff) << 8) | (0 & 0xff);
		int colorM10 = ((124 & 0xff) << 16) | ((252 & 0xff) << 8) | (50 & 0xff);
		int colorM20 = ((0 & 0xff) << 16) | ((128 & 0xff) << 8) | (0 & 0xff);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				posizioneArrayImmagine = y * width + x;
				pixSorgente = pixels1[posizioneArrayImmagine];
				// if (y == 127 && x == 127)
				// MyLog.waitHere("slice= " + slice + " pixSorgente= " +
				// pixSorgente + " mean= " + mean + " plus10= "
				// + plus10);
				if (pixSorgente > plus20) {
					aux1 = colorP20;
				} else if (pixSorgente > plus10) {
					aux1 = colorP10;
				} else if (pixSorgente > minus10) {
					aux1 = colorMED;
					// IJ.log("slice= " + slice + " x= " + x + " y= " + y + "
					// pixSorgente= " + pixSorgente);
				} else if (pixSorgente > minus20) {
					aux1 = colorM10;
				} else if (pixSorgente > 100) {
					aux1 = colorM20;
				} else {
					aux1 = colorOUT;
				}
				pixelsMappa[posizioneArrayImmagine] = aux1;
				// pixelsMappa[posizioneArrayImmagine] = 1000;
			}
		}
		ipMappa.resetMinAndMax();
		// impMappazza.setSlice(slice);
		return;
	}

	public static void mappazza5Colori(double mean11, ImagePlus imp1, ImagePlus impMappazza, int slice, int coil,
			boolean debug) {

		if (imp1 == null) {
			MyLog.waitHere("imp1==null");
			return;
		}
		if (impMappazza == null) {
			MyLog.waitHere("impMappazza==null");
			return;
		}
		IJ.log("coil= " + coil);

		int width = imp1.getWidth();
		int height = imp1.getHeight();
		short[] pixels1 = UtilAyv.truePixels(imp1);
		double mean = mean11 * 0.6;
		double minus20 = mean * MyConst.MINUS_20_PERC;
		double minus10 = mean * MyConst.MINUS_10_PERC;
		double plus10 = mean * MyConst.PLUS_10_PERC;
		double plus20 = mean * MyConst.PLUS_20_PERC;
		int colorOUT = 0;

		ImageStack stack1 = impMappazza.getStack();
		ImageProcessor ipMappa = stack1.getProcessor(slice);
		int[] pixelsMappa = (int[]) ipMappa.getPixels();

		// ImageProcessor ipSimulata = new ColorProcessor(width, height);
		// int[] pixelsSimulata = (int[]) ipSimulata.getPixels();

		short pixSorgente = 0;
		int aux1 = 0;
		int posizioneArrayImmagine = 0;
		int colorP20 = 0;
		int colorP10 = 0;
		int colorMED = 0;
		int colorM10 = 0;
		int colorM20 = 0;

		if (coil == 1) {
			colorP20 = ((255 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
			colorP10 = ((255 & 0xff) << 16) | ((30 & 0xff) << 8) | (30 & 0xff);
			colorMED = ((255 & 0xff) << 16) | ((60 & 0xff) << 8) | (60 & 0xff);
			colorM10 = ((255 & 0xff) << 16) | ((90 & 0xff) << 8) | (90 & 0xff);
			colorM20 = ((255 & 0xff) << 16) | ((120 & 0xff) << 8) | (120 & 0xff);
		}
		if (coil == 2) {
			colorP20 = ((0 & 0xff) << 16) | ((255 & 0xff) << 8) | (0 & 0xff);
			colorP10 = ((30 & 0xff) << 16) | ((255 & 0xff) << 8) | (30 & 0xff);
			colorMED = ((60 & 0xff) << 16) | ((255 & 0xff) << 8) | (60 & 0xff);
			colorM10 = ((90 & 0xff) << 16) | ((255 & 0xff) << 8) | (90 & 0xff);
			colorM20 = ((120 & 0xff) << 16) | ((255 & 0xff) << 8) | (120 & 0xff);
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				posizioneArrayImmagine = y * width + x;
				pixSorgente = pixels1[posizioneArrayImmagine];
				// if (y == 127 && x == 127)
				// MyLog.waitHere("slice= " + slice + " pixSorgente= " +
				// pixSorgente + " mean= " + mean + " plus10= "
				// + plus10);
				if (pixSorgente > plus20) {
					aux1 = colorP20;
				} else if (pixSorgente > plus10) {
					aux1 = colorP10;
				} else if (pixSorgente > minus10) {
					aux1 = colorMED;
					// IJ.log("slice= " + slice + " x= " + x + " y= " + y + "
					// pixSorgente= " + pixSorgente);
				} else if (pixSorgente > minus20) {
					aux1 = colorM10;
				} else if (pixSorgente > 100) {
					aux1 = colorM20;
				} else {
					aux1 = colorOUT;
				}
				int[] color1 = getColor(pixelsMappa[posizioneArrayImmagine]);

				int[] color2 = getColor(aux1);

				int color3 = mixColor(color1, color2);
				if (slice == 131 && x == 128 && y == 185)
					MyLog.waitHere("pixSorgente= " + pixSorgente + " aux1= " + aux1 + "\npixelsMappa= "
							+ pixelsMappa[posizioneArrayImmagine] + " color3= " + color3);

				// if (debug)
				// color3 = 1000;

				pixelsMappa[posizioneArrayImmagine] = color3;
				// pixelsMappa[posizioneArrayImmagine] = 1000;
			}
		}
		ipMappa.resetMinAndMax();
		// impMappazza.setSlice(slice);
		return;
	}

	public static int[] getColor(int pixel) {
		int[] c1 = new int[3];
		int r1, g1, b1;
		r1 = (pixel & 0xff0000) >> 16;
		g1 = (pixel & 0xff00) >> 8;
		b1 = (pixel & 0xff);
		c1[0] = r1;
		c1[1] = g1;
		c1[2] = b1;
		return c1;
	}

	public static int mixColor(int[] rgb1, int[] rgb2) {
		int red = rgb1[0] + rgb2[0];
		if (red > 255)
			red = 255;
		int green = rgb1[1] + rgb2[1];
		if (green > 255)
			green = 255;
		int blue = rgb1[2] + rgb2[2];
		if (blue > 255)
			blue = 255;
		int color = ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
		return color;
	}

} // ultima
