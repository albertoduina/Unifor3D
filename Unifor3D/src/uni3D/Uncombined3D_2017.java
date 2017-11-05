package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.geom.Rectangle2D;
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
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
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
import utils.MyGenericDialogGrid;
import utils.MyLine;
import utils.MyLog;
import utils.MyPlot;
import utils.MySphere;
import utils.MyStackUtils;
import utils.MyTimeUtils;
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

public class Uncombined3D_2017 implements PlugIn {
	static boolean debug1 = false;
	final static int timeout = 100;
	public static String VERSION = "CDQ 3D";

	public void run(String arg) {

		boolean demo0 = false;
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
		int livelli = 0;
		if (level.equals("5 livelli")) {
			twelve = false;
			livelli = 5;
		} else {
			twelve = true;
			livelli = 12;
		}

		ArrayList<Integer> pixListSignal11 = new ArrayList<Integer>();

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
		ImagePlus impUncombined = null;
		String myName = null;
		String pathUncombined = null;
		String pathCombined = null;
		// String path20 = null;
		String[] dir1a = null;
		// String[] dir2a = null;
		String dirDefaultUncombined = null;
		String dirDefaultCombined = null;
		String dir1 = null;
		// String dir2 = null;
		int num = 0;
		// int num2 = 0;
		if (auto) {
			dirDefaultUncombined = Prefs.get("prefer.Unifor3D_dir3", "none");
			DirectoryChooser.setDefaultDirectory(dirDefaultUncombined);
			DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA STACK UNCOMBINED DA ELABORARE");
			dir1 = od1.getDirectory();
			if (dir1 == null)
				return;
			Prefs.set("prefer.Unifor3D_dir3", dir1);
			dir1a = new File(dir1).list();
			num = dir1a.length;

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
			dirDefaultUncombined = Prefs.get("prefer.Unifor3D_dir3", "");
			dirDefaultUncombined = UtilAyv.dirSeparator(dirDefaultUncombined);
			OpenDialog.setDefaultDirectory(dirDefaultUncombined);
			OpenDialog dd1 = new OpenDialog("SELEZIONARE LO STACK UNCOMBINED DA ELABORARE");
			pathUncombined = dd1.getPath();
			if (pathUncombined == null)
				return;
			Prefs.set("prefer.Unifor3D_dir3", pathUncombined);
			num = 1;
			dirDefaultCombined = Prefs.get("prefer.Unifor3D_dir4", "");
			dirDefaultCombined = UtilAyv.dirSeparator(dirDefaultCombined);
			OpenDialog.setDefaultDirectory(dirDefaultCombined);
			OpenDialog dd2 = new OpenDialog("SELEZIONARE LO STACK COMBINED DI RIFERIMENTO");
			pathCombined = dd2.getPath();
			if (pathCombined == null)
				return;

			Prefs.set("prefer.Unifor3D_dir4", pathCombined);
			// num2 = 1;
		}

		int gridWidth = 2;
		int gridHeight = livelli;
		int gridSize = gridWidth * gridHeight;
		TextField[] tf2 = new TextField[gridSize];
		// String[] lab2 = new String[gridSize];
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
			// comodo il preset ????
			// displayValues2(gridSize, value2);
		}

		for (int i1 = 0; i1 < value2.length; i1++) {
			Prefs.set("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, value2[i1]);
		}

		// MyLog.resultsLog(value2, "value2");
		// MyLog.waitHere();

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
		// int countS = 0;
		// int[][] matClassi = new int[6][2];
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
		myName = impCombined.getTitle();

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

		IJ.log("===============================================================");
		IJ.log("centerSphere run 1 con coordinate generiche");
		IJ.log("===============================================================");

		// int[] out1 = threeBalls(imp27, coordinates1, demo0);
		double[] sphere1 = MySphere.centerSphere(impCombined, demo0);

		IJ.log("centro fantoccio X=" + sphere1[0] + " Y= " + sphere1[1] + " Z= " + sphere1[2]
				+ " diametro= " + sphere1[3]);

		ImagePlus impMapR = null;
		ImagePlus impMapG = null;
		ImagePlus impMapB = null;
		ImagePlus impMapRGB = null;
		ImageStack stackRGB = null;

		int width = impCombined.getWidth();
		int height = impCombined.getHeight();
		int depth = impCombined.getStackSize();
		int bitdepth = 24;
		int myColors = 3;

		boolean generate = true;
		if (generate) {
			impMapR = MySphere.generaMappazzaVuota16(width, height, depth, "mapR");
			impMapG = MySphere.generaMappazzaVuota16(width, height, depth, "mapG");
			impMapB = MySphere.generaMappazzaVuota16(width, height, depth, "mapB");
			stackRGB = ImageStack.create(width, height, depth, bitdepth);
			impMapRGB = new ImagePlus("mapRGB", stackRGB);
			generate = false;
		}

		// =========================
		// SFERA ESTERNA GRIGIA
		// =========================
		int x0 = (int) sphere1[0];
		int y0 = (int) sphere1[1];
		int z0 = (int) sphere1[2];
		int d0 = (int) sphere1[3];
		int[] colorRGB3 = { 150, 150, 150 };
		boolean surfaceonly = true;
		int[] bounds = new int[3];
		bounds[0] = width;
		bounds[1] = height;
		bounds[2] = depth;
		MySphere.addSphere(impMapR, impMapG, impMapB, sphere1, bounds, colorRGB3, surfaceonly);
		MySphere.generaMappazzaCombinata(impMapR, impMapG, impMapB, impMapRGB, myColors);
		impMapR.show();
		impMapG.show();
		impMapB.show();
		impMapRGB.show();
		// MyLog.waitHere();

		// =================================================================
		// ELABORAZIONE STACK UNCOMBINED
		// =================================================================

		int count0 = 0;
		// MyLog.waitHere("immagini da elaborare= " + num);
		int cr = 0;
		int cg = 0;
		int cb = 0;
		int aa = 0;
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

		while (count0 < num) {
			long time1 = System.nanoTime();

			aa += 1;
			if (aa == 1) {
				cr = r1;
				cg = g1;
				cb = b1;
			}
			if (aa == 2) {
				cr = r2;
				cg = g2;
				cb = b2;
			}
			if (aa == 3) {
				cr = r3;
				cg = g3;
				cb = b3;
				aa = 0;
			}

			// IJ.log("aa= " + aa + " cr= " + cr + " cg= " + cg + " cb= " + cb);
			if (auto) {
				pathUncombined = dir1 + dir1a[count0];
				IJ.log("elaborazione " + count0 + " / " + num);
			}
			// MyLog.waitHere("path10= " + path10);
			impUncombined = UtilAyv.openImageNoDisplay(pathUncombined, false);
			myName = impUncombined.getTitle();

			count0++;
			// int width = imp10.getWidth();
			// int height = imp10.getHeight();
			ImageStack imaStack = impUncombined.getImageStack();
			if (imaStack == null) {
				IJ.log("imageFromStack.imaStack== null");
				return;
			}

			if (imaStack.getSize() < 2) {
				MyLog.waitHere("Per le elaborazioni 3D ci vuole uno stack di piu'mmagini!");
				return;
			}

			int demolevel = 0;
			int diamsearch = 6;
			// int diamsearch = 14;
			double[] circularSpot = MySphere.searchCircularSpot(impUncombined, sphere1, diamsearch, "", demolevel);
			MyLog.logVector(circularSpot, "circularSpot");
			IJ.log("uncombined spot X " + count0 + " =" + circularSpot[0] + " Y= " + circularSpot[1] + " Z= "
					+ circularSpot[2]);

			int x2 = (int) circularSpot[0];
			int y2 = (int) circularSpot[1];
			int z2 = (int) circularSpot[2];
			int d2 = diamsearch;
			int[] colorRGB2 = new int[3];
			colorRGB2[0] = cr;
			colorRGB2[1] = cg;
			colorRGB2[2] = cb;
			// IJ.log("x2= " + x2 + " y2= " + y2 + " z2= " + z2 + " d2= " + d2 +
			// " cr= " + cr + " cg= " + cg + " cb= "
			// + cb);
			double[] sphere2 = new double[4];
			sphere2[0] = x2;
			sphere2[1] = y2;
			sphere2[2] = z2;
			sphere2[3] = d2;

			surfaceonly = false;
			MySphere.addSphere(impMapR, impMapG, impMapB, sphere2, bounds, colorRGB2, surfaceonly);
			MySphere.generaMappazzaCombinata(impMapR, impMapG, impMapB, impMapRGB, myColors);
			impMapR.updateAndDraw();
			impMapG.updateAndDraw();
			impMapB.updateAndDraw();
			impMapRGB.updateAndDraw();


			double[] vetpixel = MySphere.vectorizeSphericalSpot(impUncombined, sphere1, sphere2, demolevel);
			int len1 = vetpixel.length;
			double mean1 = ArrayUtils.vetMean(vetpixel);
			IJ.log("volume effettivo sfera  = " + len1 + "[voxels]");
			IJ.log("mean sfera " + count0 + " = " + mean1);

			// MyLog.waitHere();
			long time2 = System.nanoTime();
			String tempo1 = MyTimeUtils.stringNanoTime(time2 - time1);
			IJ.log("Tempo sfera " + count0 + "   hh:mm:ss.ms " + tempo1);

			if (false) {
				// al contrario dell'uniformita' non mi baso su una
				// ricostruzione
				// geometrica dela sfera ma effettuo la ricerca dello spot11x11
				// su
				// tutte le sezioni, quindi i dati geometrici servono a una
				// cippa
				double profond = 30;
				int mode = 0;
				ImageStack newStack = new ImageStack(impUncombined.getWidth(), impUncombined.getHeight());

				for (int i1 = 0; i1 < impUncombined.getImageStackSize(); i1++) {
					if (!auto)
						IJ.log("localizzo hotspot " + i1 + " / " + impUncombined.getImageStackSize());
					ImagePlus imp20 = MyStackUtils.imageFromStack(impUncombined, i1 + 1);
					double[] pos20 = positionSearch(imp20, profond, "", mode, timeout);
					if (pos20 == null) {
						continue;
					}
					double diamMROI = 11;
					double xCenterRoi = pos20[0];
					double yCenterRoi = pos20[1];
					ImagePlus imp21 = MyStackUtils.imageFromStack(impUncombined, i1 + 1);
					pixVectorize(imp21, xCenterRoi, yCenterRoi, diamMROI, pixListSignal11);
					// IJ.wait(timeout);
					imp20.close();
					imp21.close();
				}
				int[] pixListSignal = ArrayUtils.arrayListToArrayInt(pixListSignal11);
				double mean11 = ArrayUtils.vetMean(pixListSignal);
				int count = -1;
				for (int i1 = 0; i1 < impUncombined.getImageStackSize(); i1++) {
					count++;
					if (!auto)
						IJ.log("calcolo simulata " + i1 + " / " + impUncombined.getImageStackSize());
					ImagePlus imp20 = MyStackUtils.imageFromStack(impUncombined, i1 + 1);
					ImagePlus impSimulata = null;

					// if (twelve) {
					// impSimulata = ImageUtils.generaSimulata12colori(mean11,
					// imp20, step2, demo0, test);
					// } else {

					impSimulata = ImageUtils.generaSimulataMultiColori(mean11, imp20, minimi, massimi, myColor);

					// impSimulata = ImageUtils.generaSimulata5Colori(mean11,
					// imp20, step2, demo0, test);
					// }

					ImageProcessor ipSimulata = impSimulata.getProcessor();
					if (count == 0)
						newStack.update(ipSimulata);
					String sliceInfo1 = impSimulata.getTitle();
					String sliceInfo2 = (String) impSimulata.getProperty("Info");
					// aggiungo i dati header alle singole immagini dello stack
					if (sliceInfo2 != null)
						sliceInfo1 += "\n" + sliceInfo2;
					newStack.addSlice(sliceInfo2, ipSimulata);

					// MyLog.waitHere("thisPos= " + thisPos + " project= " +
					// project
					// +
					// "\ndiamEXT2= " + diamEXT2 + " diamMROI2= "
					// + diamMROI2);
					// MyLog.waitHere();

					ImageWindow iwSimulata = impSimulata.getWindow();
					if (iwSimulata != null)
						iwSimulata.dispose();

					impSimulata.close();

				}

				ImagePlus simulataStack = new ImagePlus("STACK_IMMAGINI_SIMULATE", newStack);
				ImagePlus impColors = ImageUtils.generaScalaColori(myColor, myLabels);
				impColors.show();

				simulataStack.show();

			}
			if (false) {
				Path path100 = Paths.get(dirDefaultUncombined);
				Path path101 = path100.getParent();

				String lev = null;
				if (twelve)
					lev = "12_livelli";
				else
					lev = "5_livelli";
				// boolean ok1 = createDirectory(path101 + "\\simul_" + lev +
				// "\\");
				// String aux1 = path101 + "\\simul_" + lev + "\\" + myName +
				// "sim";
				// // MyLog.waitHere("aux1= " + aux1);
				// new FileSaver(simulataStack).saveAsTiff(aux1);
				// String aux2 = path101 + "\\simul_" + lev + "\\" + "colori_" +
				// "sim";
				// new FileSaver(impColors).saveAsTiff(aux2);

				while (WindowManager.getWindowCount() > 0) {
					IJ.wait(100);
					IJ.run("Close");
					IJ.wait(100);
				}
			}

		}
		int num1;

		String lev = null;
		if (twelve) {
			lev = "12_livelli";
			num1 = 12;
		} else {
			lev = "5_livelli";
			num1 = 5;

			// Path path100 = Paths.get(dir10);
			// Path path101 = path100.getParent();
			//// ImagePlus scala = ImageUtils.generaScalaColori(num1);
			// String aux2 = path101 + "\\simul_" + lev + "\\" + myName +
			// "scala";
			// // MyLog.waitHere("aux1= " + aux1);
			// new FileSaver(scala).saveAsTiff(aux2);
			// UtilAyv.cleanUp2();
		}
		long time4 = System.nanoTime();
		String tempo2 = MyTimeUtils.stringNanoTime(time4 - time3);
		IJ.log("Tempo totale  hh:mm:ss.ms " + tempo2);

		MyLog.waitHere("FINE");

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

			MyLog.waitHere(listaMessaggi(5), debug1, timeout);

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

	public static double[] positionSearch(ImagePlus imp11, double profond, String info1, int mode, int timeout) {
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

	/**
	 * Ricerca posizione ROI per calcolo uniformita'. Versione con Canny Edge
	 * Detector. Questa versione è da utilizzare per il fantoccio sferico
	 */

	public static ImagePlus cannyFilter(ImagePlus imp11, float gaussianKernelRadius, float lowThreshold,
			float highThreshold, boolean contrastnormalized, int mode, int timeout1) {

		// ================================================================================
		// Inizio calcoli geometrici
		// ================================================================================
		//
		// -----------------------------------------------------
		// Settaggi per immagini uncombined
		// -----------------------------------------------------
		MyCannyEdgeDetector mce = new MyCannyEdgeDetector();
		// optima
		// mce.setGaussianKernelRadius(4.0f);
		// mce.setLowThreshold(23.0f);
		// mce.setHighThreshold(24.0f);
		// mce.setContrastNormalized(true);

		mce.setGaussianKernelRadius(gaussianKernelRadius);
		mce.setLowThreshold(lowThreshold);
		mce.setHighThreshold(highThreshold);
		mce.setContrastNormalized(contrastnormalized);
		IJ.log("threeBalls: CannyEdgeDetector, gaussianKernelRadius= " + gaussianKernelRadius + " lowThreshold= "
				+ lowThreshold + " highTreshold= " + highThreshold + " contrastnormalized=" + contrastnormalized);

		ImagePlus imp12 = mce.process(imp11);
		return imp12;
	}

	/**
	 * Ricerca posizione ROI per calcolo uniformita'. Versione con Canny Edge
	 * Detector. Questa versione è da utilizzare per il fantoccio sferico
	 */

	public static int[][] cannyPointsExtractor(ImagePlus imp12) {

		// ================================================================================
		// Inizio calcoli geometrici
		// ================================================================================
		//
		IJ.log("threeBalls: cannyPointsExtractor");

		ImageProcessor ip12 = imp12.getProcessor();
		byte[] pixels = (byte[]) ip12.getPixelsCopy();
		int offset = 0;
		ArrayList<Integer> matrixelement1 = new ArrayList<Integer>();
		ArrayList<Integer> matrixelement2 = new ArrayList<Integer>();
		for (int y1 = 0; y1 < imp12.getHeight(); y1++) {
			offset = y1 * imp12.getWidth();
			for (int x1 = 0; x1 < imp12.getWidth(); x1++) {
				if (pixels[offset + x1] == (byte) 255) {
					matrixelement1.add(x1);
					matrixelement2.add(y1);
					ImageUtils.setOverlayPixels(imp12, x1, y1, Color.red, Color.green, true);
				}
			}
		}
		int[] matrix1 = ArrayUtils.arrayListToArrayInt(matrixelement1);
		int[] matrix2 = ArrayUtils.arrayListToArrayInt(matrixelement2);
		int[][] matrix = new int[2][matrix1.length];
		for (int i1 = 0; i1 < matrix1.length; i1++) {
			matrix[0][i1] = matrix1[i1];
			matrix[1][i1] = matrix2[i1];
		}
		return matrix;
	}

	/**
	 * fitQuality Restituisce la percentuale dei pixel da fittare coincidenti
	 * effettivamente col cerchio affittato
	 * 
	 * @param imp1
	 * @param total
	 *            numero pixels da fittare
	 * @return
	 */
	public static double fitQualityOLD(ImagePlus imp1, int total) {

		double conta = 0;

		ImagePlus imp2 = imp1.duplicate();
		ImageProcessor ip2 = imp2.getProcessor();
		imp2.deleteRoi();
		ip2.setValue(0);
		ip2.fill();

		ImageProcessor ip1 = imp1.getProcessor();
		Roi circle1 = imp1.getRoi();
		if (circle1 == null) {
			MyLog.waitHere("circle1= null");
			return -1;
		}

		circle1.drawPixels(ip2);

		Polygon fpol1 = circle1.getPolygon();
		// 1FloatPolygon fpol1 = circle1.getFloatPolygon();
		// 2 FloatPolygon fpol1 = circle1.getInterpolatedPolygon();
		// 1 FloatPolygon fpol1 = circle1.getInterpolatedPolygon(1.0, true);

		int aux1 = 0;
		int xx = 0;
		int yy = 0;

		for (int i1 = 0; i1 < fpol1.npoints; i1++) {
			// xx = (int) Math.round(fpol1.xpoints[i1]);
			// yy = (int) Math.round(fpol1.ypoints[i1]);
			xx = (int) fpol1.xpoints[i1];
			yy = (int) fpol1.ypoints[i1];

			aux1 = ip1.getPixel(xx, yy);
			if (aux1 == 255) {
				conta++;
				ImageUtils.setOverlayPixels(imp1, xx, yy, Color.green, Color.red, true);
			}
		}

		imp1.updateAndDraw();

		double percentPixGood = (conta / (double) total) * 100.0;
		IJ.log("percento= " + IJ.d2s(percentPixGood, 2) + "% conta= " + conta + " totale= " + total);
		if (percentPixGood < 20.0)
			MyLog.waitHere("accettable pixel percentage < 20");
		return percentPixGood;
	}

	/**
	 * fitQuality Restituisce la percentuale dei pixel da fittare coincidenti
	 * effettivamente col cerchio affittato
	 * 
	 * @param imp1
	 *            immagine da analizzare
	 * @param total
	 *            numero pixels da fittare
	 * @return
	 */
	public static double fitQuality(ImagePlus imp1, int total) {

		double conta = 0;

		// prelevo la roi circolare disegnata sull'immagine da analizzare, si
		// chiama circle1
		ImageProcessor ip1 = imp1.getProcessor();
		Roi circle1 = imp1.getRoi();
		if (circle1 == null) {
			MyLog.waitHere("circle1= null");
			return -1;
		}

		imp1.deleteRoi();
		// disegno la stessa roi su di una immagine tutta a 255
		ImageProcessor ip2 = new ByteProcessor(imp1.getWidth(), imp1.getHeight());
		// ImagePlus imp2 = new ImagePlus("fittedCircleDrawPixels", ip2);
		// imp2.show();
		ip2.setValue(255);
		circle1.drawPixels(ip2);
		// imp2.updateAndDraw();

		byte[] pixels1 = (byte[]) ip1.getPixels();
		byte[] pixels2 = (byte[]) ip2.getPixels();
		short p1 = 0;
		short p2 = 0;
		int offset = 0;
		// conteggio i pixel a 255 in entrambe le immagini
		for (int y1 = 0; y1 < imp1.getHeight(); y1++) {
			offset = y1 * imp1.getWidth();
			for (int x1 = 0; x1 < imp1.getWidth(); x1++) {
				p1 = pixels1[offset + x1];
				p2 = pixels2[offset + x1];
				if (p1 != 0 && p2 != 0) {
					conta++;
					ImageUtils.setOverlayPixels(imp1, x1, y1, Color.green, Color.red, true);
				}
			}
		}
		imp1.updateAndDraw();
		double percentPixGood = (conta / (double) total) * 100.0;
		// IJ.log("percento= " + IJ.d2s(percentPixGood, 2) + "% conta= " + conta
		// + " totale= " + total);
		// MyLog.waitHere("percento= " + IJ.d2s(percentPixGood, 2) + "% conta= "
		// + conta + " totale= " + total);
		return percentPixGood;
	}

	/***
	 * ThreeBalls: ricerca posizione del centro sfera PER LA IMMAGINE COMBINED.
	 * Si utilizza Orthogonal Views per ricostruire le due viste laterali dello
	 * stack. Applicando il Canny Edge Detector si ricava il profilo circolare
	 * delle immagini, se ne selezionano alcuni punti e su questi viene eseguito
	 * il fit dell'equazione del cerchio con la routine fitCircle, determinando
	 * cosi' diametro e coordinate del centro (XZ per una vista ed YZ per
	 * l'altra). Di seguito a fitCircle viene calcolata una fitQuality, la quale
	 * rappresenta la percentuale dei pixels di CannyEdgeDetector che combaciano
	 * perfettamente con il cerchio calcolato da fitCircle (valori accettabili
	 * al di sopra del 20% >>>> EL DISE ME E BASTA !! <<<<<). Ricaviamo cosi' XZ
	 * ed YZ, le due coordinate Z dovrebbero coincidere, ne facciamo in ogni
	 * caso la media.
	 * =======================================================================
	 * si potrebbe ripetere il tutto, dopo aver impostato il centro con
	 * setCrossLoc, ottenendo cosi' le tre viste ricavate dal centro che abbiamo
	 * fornito, su queste possiamo rifare i calcoli ed ottenere anche i diametri
	 * delle tre viste. Poi qualcuno mi raccontera' dove, come ed in che
	 * direzioni andare a calcolare la DGP di una sfera, non so se la NEMA ci ha
	 * pensato.
	 * =========================================================================
	 * 
	 * 
	 * La routine ThreeBalls restituisce le coordinate XYZ ed il diametro della
	 * sfera. Il nome sarebbe stato duPalle ma si voleva esagerare.
	 * 
	 * 
	 * @param imp1
	 * @param coordinates
	 *            int[] xCenter, yCenter, zCenter, diameter
	 * @return int[] xCenter, yCenter, zCenter, diameter
	 * 
	 */
	public static int[] threeBalls(ImagePlus imp1, int[] coordinates, boolean demo0) {

		Color colore1 = Color.red;
		Color colore2 = Color.green;
		// Color colore3 = Color.red;
		int[] centroSfera = null;

		boolean showProfiles = false;
		// boolean manual = false;

		MyLog.logVector(coordinates, "threeBalls.coordinates_input");

		// x=128, y=128, z=0 ottenuti da getCrossLoc se non passo niente
		IJ.run(imp1, "Orthogonal Views", "");
		Orthogonal_Views ort1 = Orthogonal_Views.getInstance();

		ort1.setCrossLoc(coordinates[0], coordinates[1], coordinates[2]);
		// int[] cross = ort1.getCrossLoc();
		// MyLog.logVector(cross, "cross");
		// if (step)
		// MyLog.waitHere("output di 'Orthogonal Views'");
		ImagePlus imp2 = ort1.getXZImage();
		if (imp2 == null)
			MyLog.waitHere("imp2=null");
		IJ.wait(100);
		ImagePlus imp12 = new Duplicator().run(imp2);
		IJ.wait(10);

		ImagePlus imp3 = ort1.getYZImage();
		if (imp3 == null)
			MyLog.waitHere("imp13=null");
		ImagePlus imp13 = new Duplicator().run(imp3);
		IJ.wait(10);

		Orthogonal_Views.stop();
		Overlay over2 = new Overlay();
		Overlay over3 = new Overlay();
		Overlay over212 = new Overlay();
		Overlay over213 = new Overlay();
		Overlay over12 = new Overlay();

		imp2.setOverlay(over2);
		imp3.setOverlay(over3);
		imp12.setOverlay(over12);

		imp12.setTitle("XZ_IMP_12");
		imp13.setTitle("YZ_IMP_13");
		int mode = 3;
		int timeout1 = 200;
		//// ====================================================
		float gaussianKernelRadius = 2.7f;
		float lowThreshold = 24.0f;
		float highThreshold = 26.0f;
		boolean contrastNormalized = true;
		ImagePlus imp212 = cannyFilter(imp12, gaussianKernelRadius, lowThreshold, highThreshold, contrastNormalized,
				mode, timeout1);
		imp212.setOverlay(over212);
		imp212.show();
		int[][] out212 = cannyPointsExtractor(imp212);
		imp212.updateAndDraw();
		int[] xPoints2 = new int[out212[0].length];
		int[] yPoints2 = new int[out212[0].length];
		for (int i1 = 0; i1 < out212[0].length; i1++) {
			xPoints2[i1] = out212[0][i1];
			yPoints2[i1] = out212[1][i1];
		}

		MyLog.waitHere();

		// double[][] peaks9 = new double[4][1];
		// double[][] peaks10 = new double[4][1];
		// double[][] peaks11 = new double[4][1];
		// double[][] peaks12 = new double[4][1];

		// ------ riadattamento da p10

		double[][] myPeaks = new double[4][1];
		int[] myXpoints = new int[16];
		int[] myYpoints = new int[16];

		int[] xcoord = new int[2];
		int[] ycoord = new int[2];

		int[] vetx0 = new int[8];
		int[] vetx1 = new int[8];
		int[] vety0 = new int[8];
		int[] vety1 = new int[8];

		int width = imp1.getWidth();
		int height = imp1.getHeight();

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
		boolean vertical = false;
		// boolean valido = true;
		for (int i1 = 0; i1 < 8; i1++) {
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

			myPeaks = cannyProfileAnalyzer2(imp12, vetTitle[i1], showProfiles, demo0, debug1, vertical, timeout);

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

					count++;
					myXpoints[count] = (int) (myPeaks[3][i2]);
					myYpoints[count] = (int) (myPeaks[4][i2]);
					ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[3][i2]), (int) (myPeaks[4][i2]), colore1, 1);
					ImageUtils.plotPoints(imp12, over12, (int) (myPeaks[3][i2]), (int) (myPeaks[4][i2]), colore1, 0);
					imp12.updateAndDraw();
					ImageUtils.imageToFront(imp12);

					// MyLog.logVector(myXpoints, "myXpoints");
					// MyLog.logVector(myYpoints, "myYpoints");
				}
			}
		}
		if (demo0)
			MyLog.waitHere("Si tracciano ulteriori linee", debug1, timeout);

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
			// ImageWindow iw111 = imp11.getWindow();
			// if (iw111 != null)
			// iw111.dispose();

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
			MyLog.waitHere("La circonferenza risultante dal fit e' mostrata in rosso", debug1, timeout1);
		Rectangle boundRec = imp12.getProcessor().getRoi();
		// xCenterCircle = Math.round(boundRec.x + boundRec.width / 2);
		// yCenterCircle = Math.round(boundRec.y + boundRec.height / 2);
		// diamCircle = boundRec.width;
		// if (!manualOverride)
		// writeStoredRoiData(boundRec);

		// MyCircleDetector.drawCenter(imp12, over12, xCenterCircle,
		// yCenterCircle, colore3);
		// if (demo1)
		// MyLog.waitHere("002\nLa circonferenza risultante dal fit e'
		// mostrata in rosso ed ha \nxCenterCircle= "
		// + xCenterCircle + " yCenterCircle= " + yCenterCircle + "
		// diamCircle= " + diamCircle);

		// =======================================================
		PointRoi pr212 = new PointRoi(xPoints2, yPoints2, xPoints2.length);
		imp212.setRoi(pr212);
		imp212.getRoi().setStrokeColor(Color.blue);
		// over212.add(imp212.getRoi());
		IJ.log("threeBalls: fitCircle");

		double[] out1 = ImageUtils.fitCircleNew(imp212);

		MyLog.logVector(out1, "fitCircleNew out1");

		imp212.updateAndDraw();
		Rectangle boundRec2 = imp212.getProcessor().getRoi();
		int xCenterCircle2 = Math.round(boundRec2.x + boundRec2.width / 2);
		int yCenterCircle2 = Math.round(boundRec2.y + boundRec2.height / 2);
		int diamCircle2 = boundRec2.width;
		imp2.setRoi(new OvalRoi(xCenterCircle2 - diamCircle2 / 2, yCenterCircle2 - diamCircle2 / 2, diamCircle2,
				diamCircle2));

		imp212.setRoi(new OvalRoi(xCenterCircle2 - diamCircle2 / 2, yCenterCircle2 - diamCircle2 / 2, diamCircle2,
				diamCircle2));
		imp212.getRoi().setStrokeColor(Color.yellow);
		// over212.add(imp212.getRoi());
		double val2 = fitQuality(imp212, out212[0].length);
		imp212.updateAndDraw();
		if (val2 < 20)
			IJ.log("ATTENZIONE fit quality <20  = " + val2);

		IJ.log("My_fit_quality = " + val2);

		// MyLog.waitHere(
		// "XZ_IMP12 Good canny fitted pixels in green,\nbad canny fitted
		// pixels
		// in red,\ninterpolation used points blue,\nfitted circle yellow");
		//// ====================================================
		ImagePlus imp213 = cannyFilter(imp13, gaussianKernelRadius, lowThreshold, highThreshold, contrastNormalized,
				mode, timeout1);
		imp213.show();
		imp213.setOverlay(over213);
		int[][] out213 = cannyPointsExtractor(imp213);
		imp213.show();
		Rectangle boundRec3 = imp213.getProcessor().getRoi();
		int xCenterCircle3 = Math.round(boundRec3.x + boundRec3.width / 2);
		int yCenterCircle3 = Math.round(boundRec3.y + boundRec3.height / 2);
		int diamCircle3 = boundRec3.width;
		imp3.setRoi(new OvalRoi(xCenterCircle3 - diamCircle3 / 2, yCenterCircle3 - diamCircle3 / 2, diamCircle3,
				diamCircle3));
		imp3.getRoi().setStrokeColor(Color.green);
		// over3.add(imp3.getRoi());
		imp213.setRoi(new OvalRoi(xCenterCircle3 - diamCircle3 / 2, yCenterCircle3 - diamCircle3 / 2, diamCircle3,
				diamCircle3));
		imp213.getRoi().setStrokeColor(Color.green);

		double val3 = fitQuality(imp213, out213[0].length);
		if (val3 < 20)
			MyLog.waitHere("fit quality < 20= " + val3);

		// MyLog.waitHere(
		// "YZ_IMP13 Good canny fitted pixels in green,\nbad canny fitted
		// pixels
		// in red,\ninterpolation used points blue,\nfitted circle yellow");

		int zeta = (yCenterCircle2 + xCenterCircle3) / 2;

		IJ.log("CENTRO SFERA");
		IJ.log("immagine XZ: x= " + xCenterCircle2 + " z= " + yCenterCircle2 + " d= " + diamCircle2 + " quality= "
				+ IJ.d2s(val2, 2) + "[ok se >20]");
		IJ.log("immagine YZ: y= " + yCenterCircle3 + " z= " + xCenterCircle3 + " d= " + diamCircle3 + " quality= "
				+ IJ.d2s(val3, 2) + "[ok se >20]");
		IJ.log("coordinate centro  x= " + xCenterCircle2 + " y= " + yCenterCircle3 + " z= " + zeta);

		int radius = (diamCircle2 + diamCircle3) / 4;
		centroSfera = new int[4];
		centroSfera[0] = xCenterCircle2;
		centroSfera[1] = yCenterCircle3;
		centroSfera[2] = zeta;
		centroSfera[3] = radius;

		return centroSfera;
	}

	public static int[] stackHotcircleSearch(ImagePlus imp1, int[] coordinates) {

		MyLog.logVector(coordinates, "threeBalls.coordinates");

		// x=128, y=128, z=0 ottenuti da getCrossLoc se non passo niente
		IJ.run(imp1, "Orthogonal Views", "");
		Orthogonal_Views ort1 = Orthogonal_Views.getInstance();

		ort1.setCrossLoc(coordinates[0], coordinates[1], coordinates[2]);
		// int[] cross = ort1.getCrossLoc();
		// MyLog.logVector(cross, "cross");
		// if (step)
		// MyLog.waitHere("output di 'Orthogonal Views'");
		ImagePlus imp2 = ort1.getXZImage();
		if (imp2 == null)
			MyLog.waitHere("imp2=null");
		IJ.wait(100);
		ImagePlus imp12 = new Duplicator().run(imp2);
		IJ.wait(10);

		ImagePlus imp3 = ort1.getYZImage();
		if (imp3 == null)
			MyLog.waitHere("imp13=null");
		ImagePlus imp13 = new Duplicator().run(imp3);
		IJ.wait(10);

		Orthogonal_Views.stop();
		Overlay over2 = new Overlay();
		Overlay over3 = new Overlay();
		Overlay over212 = new Overlay();
		Overlay over213 = new Overlay();
		imp2.setOverlay(over2);
		imp3.setOverlay(over3);
		imp12.setTitle("XZ_IMP_12");
		imp13.setTitle("YZ_IMP_13");
		int mode = 3;
		int timeout1 = 200;
		//// ====================================================
		float gaussianKernelRadius = 2.7f;
		float lowThreshold = 24.0f;
		float highThreshold = 26.0f;
		boolean contrastNormalized = true;
		ImagePlus imp212 = cannyFilter(imp12, gaussianKernelRadius, lowThreshold, highThreshold, contrastNormalized,
				mode, timeout1);
		imp212.setOverlay(over212);
		imp212.show();
		int[][] out212 = cannyPointsExtractor(imp212);
		imp212.updateAndDraw();
		int[] xPoints2 = new int[out212[0].length];
		int[] yPoints2 = new int[out212[0].length];
		for (int i1 = 0; i1 < out212[0].length; i1++) {
			xPoints2[i1] = out212[0][i1];
			yPoints2[i1] = out212[1][i1];
		}
		PointRoi pr212 = new PointRoi(xPoints2, yPoints2, xPoints2.length);
		imp212.setRoi(pr212);
		imp212.getRoi().setStrokeColor(Color.blue);
		// over212.add(imp212.getRoi());
		IJ.log("threeBalls: fitCircle");

		double[] out1 = ImageUtils.fitCircleNew(imp212);

		MyLog.logVector(out1, "fitCircleNew out1");

		imp212.updateAndDraw();
		Rectangle boundRec2 = imp212.getProcessor().getRoi();
		int xCenterCircle2 = Math.round(boundRec2.x + boundRec2.width / 2);
		int yCenterCircle2 = Math.round(boundRec2.y + boundRec2.height / 2);
		int diamCircle2 = boundRec2.width;
		imp2.setRoi(new OvalRoi(xCenterCircle2 - diamCircle2 / 2, yCenterCircle2 - diamCircle2 / 2, diamCircle2,
				diamCircle2));

		imp212.setRoi(new OvalRoi(xCenterCircle2 - diamCircle2 / 2, yCenterCircle2 - diamCircle2 / 2, diamCircle2,
				diamCircle2));
		imp212.getRoi().setStrokeColor(Color.yellow);
		// over212.add(imp212.getRoi());
		double val2 = fitQuality(imp212, out212[0].length);
		imp212.updateAndDraw();
		if (val2 < 20)
			IJ.log("ATTENZIONE fit quality <20  = " + val2);

		IJ.log("My_fit_quality = " + val2);

		// MyLog.waitHere(
		// "XZ_IMP12 Good canny fitted pixels in green,\nbad canny fitted pixels
		// in red,\ninterpolation used points blue,\nfitted circle yellow");
		//// ====================================================
		ImagePlus imp213 = cannyFilter(imp13, gaussianKernelRadius, lowThreshold, highThreshold, contrastNormalized,
				mode, timeout1);
		imp213.show();
		imp213.setOverlay(over213);
		int[][] out213 = cannyPointsExtractor(imp213);
		imp213.show();
		int[] xPoints3 = new int[out213[0].length];
		int[] yPoints3 = new int[out213[0].length];
		for (int i1 = 0; i1 < out213[0].length; i1++) {
			xPoints3[i1] = out213[0][i1];
			yPoints3[i1] = out213[1][i1];
		}
		PointRoi pr213 = new PointRoi(xPoints3, yPoints3, xPoints3.length);
		imp213.setRoi(pr213);
		double[] out2 = ImageUtils.fitCircleNew(imp213);
		MyLog.logVector(out2, "fitCircleNew out2");

		imp213.show();
		Rectangle boundRec3 = imp213.getProcessor().getRoi();
		int xCenterCircle3 = Math.round(boundRec3.x + boundRec3.width / 2);
		int yCenterCircle3 = Math.round(boundRec3.y + boundRec3.height / 2);
		int diamCircle3 = boundRec3.width;
		imp3.setRoi(new OvalRoi(xCenterCircle3 - diamCircle3 / 2, yCenterCircle3 - diamCircle3 / 2, diamCircle3,
				diamCircle3));
		imp3.getRoi().setStrokeColor(Color.green);
		// over3.add(imp3.getRoi());
		imp213.setRoi(new OvalRoi(xCenterCircle3 - diamCircle3 / 2, yCenterCircle3 - diamCircle3 / 2, diamCircle3,
				diamCircle3));
		imp213.getRoi().setStrokeColor(Color.green);
		double val3 = fitQuality(imp213, out213[0].length);
		if (val3 < 20)
			MyLog.waitHere("fit quality < 20= " + val3);

		// MyLog.waitHere(
		// "YZ_IMP13 Good canny fitted pixels in green,\nbad canny fitted pixels
		// in red,\ninterpolation used points blue,\nfitted circle yellow");

		int zeta = (yCenterCircle2 + xCenterCircle3) / 2;

		IJ.log("CENTRO SFERA");
		IJ.log("immagine XZ: x= " + xCenterCircle2 + " z= " + yCenterCircle2 + " d= " + diamCircle2 + " quality= "
				+ IJ.d2s(val2, 2) + "[ok se >20]");
		IJ.log("immagine YZ: y= " + yCenterCircle3 + " z= " + xCenterCircle3 + " d= " + diamCircle3 + " quality= "
				+ IJ.d2s(val3, 2) + "[ok se >20]");
		IJ.log("coordinate centro  x= " + xCenterCircle2 + " y= " + yCenterCircle3 + " z= " + zeta);
		int[] centroSfera = new int[3];
		centroSfera[0] = xCenterCircle2;
		centroSfera[1] = yCenterCircle3;
		centroSfera[2] = zeta;
		return centroSfera;
	}

	public static double testChi2(double[] expected, double[] observed) {

		IJ.log("CHIxCHI= CHIQUADRO,  CHIxCHIxCHI= CHICUBO");
		return 99999.000001;
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
		imp1.show();
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

} // ultima
