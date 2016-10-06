package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
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
import ij.util.Tools;
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

public class Uncombined3D_MAPPAZZA implements PlugIn {
	static boolean debug = false;
	static boolean stampa = true;
	static boolean stampa2 = true;
	static int debugXX = 120;
	static int debugYY = 90;
	static int debugZZ = 80;
	static int puntatore = 0;

	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = false;
	public static String VERSION = "CDQ 3D";

	public void run(String arg) {

		int width = 0;
		int height = 0;
		new AboutBox().about("Uncombined3D", MyVersion.CURRENT_VERSION);
		IJ.wait(20);
		new AboutBox().close();
		String def1 = Prefs.get("prefer.Uncombined3D_MAPPAZZA_def1", "5");
		String def2 = Prefs.get("prefer.Uncombined3D_MAPPAZZA_def2", "5");
		String def3 = Prefs.get("prefer.Uncombined3D_MAPPAZZA_def3", "3");
		// boolean sat1 = Prefs.get("prefer.Uncombined3D_MAPPAZZA_sat1", true);
		boolean all1 = Prefs.get("prefer.Uncombined3D_MAPPAZZA_all1", true);
		// boolean myTest1 = Prefs.get("prefer.Uncombined3D_MAPPAZZA_MyTest",
		// true);

		boolean buco = false;

		GenericDialog gd = new GenericDialog("", IJ.getInstance());
		String[] livelli = { "12", "11", "10", "9", "8", "7", "6", "5", "4", "3", "2", "1" };
		gd.addChoice("LIVELLI SIMULATE", livelli, def1);
		String[] lati = { "19", "17", "15", "13", "11", "9", "7", "5", "3" };
		gd.addChoice("LATO HOTCUBE", lati, def2);

		gd.addCheckbox("ALL COILS", all1);
		String[] colors = { "1", "2", "3" };
		gd.addChoice("ALGO. COLORI", colors, def3);

		// gd.addCheckbox("SATURATED COLORS", sat1);
		// gd.addCheckbox("MyTest", myTest1);
		gd.addCheckbox("debug", false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		String level = gd.getNextChoice();
		String lato1 = gd.getNextChoice();
		String colors1 = gd.getNextChoice();
		boolean tutte = gd.getNextBoolean();
		// myTest = gd.getNextBoolean();
		debug = gd.getNextBoolean();
		int livello = Integer.parseInt(level);
		int latoHotCube = Integer.parseInt(lato1);
		int myColors = Integer.parseInt(colors1);

		Prefs.set("prefer.Uncombined3D_MAPPAZZA_def1", level);
		Prefs.set("prefer.Uncombined3D_MAPPAZZA_def2", lato1);
		Prefs.set("prefer.Uncombined3D_MAPPAZZA_def3", colors1);
		// Prefs.set("prefer.Uncombined3D_MAPPAZZA_sat1", colors1);
		Prefs.set("prefer.Uncombined3D_MAPPAZZA_all1", tutte);

		int gridWidth = 2;
		int gridHeight = livello;
		int gridSize = gridWidth * gridHeight;
		TextField[] tf2 = new TextField[gridSize];
		// String[] lab2 = new String[gridSize];
		String[] lab2 = { "min% classe 1", "max% classe 1", "min% classe 2", "max% classe 2", "min% classe 3",
				"max% classe 3", "min% classe 4", "max% classe 4", "min% classe 5", "max% classe 5", "min% classe 6",
				"max% classe 6", "min% classe7", "max% classe 7", "min% classe 8", "max% classe 8", "min% classe 9",
				"max% classe 9", "min% classe 10", "max% classe 10", "min% classe 11", "max% classe 11",
				"min% classe 12", "max% classe 12" };
		double[] value2 = new double[gridSize];

		MyGenericDialogGrid mgdg = new MyGenericDialogGrid();

		for (int i1 = 0; i1 < value2.length; i1++) {
			value2[i1] = mgdg.getValue2(Prefs.get("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, "0"));
		}

		int decimals = 0;
		String title2 = "LIMITI CLASSI PIXELS";
		if (mgdg.showDialog2(gridWidth, gridHeight, tf2, lab2, value2, title2, decimals)) {
			// displayValues2(gridSize, value2);
		}

		for (int i1 = 0; i1 < value2.length; i1++) {
			Prefs.set("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, value2[i1]);
		}

		// MyLog.resultsLog(value2, "value2");
		// MyLog.waitHere();

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
		if (buco) MyLog.waitHere("LO SAI CHE LE CLASSI IMPOSTATE HANNO UN BUCO ?");

		// MyLog.resultsLog(minimi, "minimi");
		// MyLog.resultsLog(massimi, "massimi");
		// MyLog.waitHere();

		IJ.log("-----IW2AYV----");
		UtilAyv.logResizer(200, 200, 400, 400);
		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}
		ImagePlus imp10 = null;
		String path10 = null;
		int colorCoil = 0;
		boolean loop1 = true;
		ImagePlus impMappazzaR = null;
		ImagePlus impMappazzaG = null;
		ImagePlus impMappazzaB = null;
		ImagePlus impMappazzaOUT = null;
		ImageStack newStackOUT = null;
		String[] dir1a = null;
		String[] dir1b = null;
		boolean generate = true;

		if (tutte) {
			DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA STACK");
			String dir1 = od1.getDirectory();
			dir1a = new File(dir1).list();
			dir1b = new String[dir1a.length];
			for (int i1 = 0; i1 < dir1a.length; i1++) {
				dir1b[i1] = dir1 + "\\" + dir1a[i1];
			}
		}

		int conta2 = -1;
		boolean vedo = false;

		while (loop1) {
			if (tutte) {
				conta2++;
				if (conta2 >= dir1b.length) {
					loop1 = false;
					continue;
				}
				path10 = dir1b[conta2];
				IJ.log("carico stack " + (conta2 + 1) + " / " + dir1b.length);
				imp10 = UtilAyv.openImageNoDisplay(path10, false);
				if (imp10 == null) {
					MyLog.waitHere("imp10==null");
					continue;
				}
				if (imp10.getBytesPerPixel() != 2) {
					continue;
				}
			} else {
				path10 = UtilAyv.imageSelection("SELEZIONARE LO STACK DA ELABORARE");
				if (path10 == null) {
					loop1 = false;
					continue;
				}
			}

			colorCoil++;
			if (colorCoil > 3)
				colorCoil = 1;

			imp10 = UtilAyv.openImageNoDisplay(path10, false);
			width = imp10.getWidth();
			height = imp10.getHeight();
			if (generate) {
				impMappazzaR = generaMappazzaVuota16(width, height, imp10.getImageStackSize(), livello, "impMappazzaR");
				impMappazzaG = generaMappazzaVuota16(width, height, imp10.getImageStackSize(), livello, "impMappazzaG");
				impMappazzaB = generaMappazzaVuota16(width, height, imp10.getImageStackSize(), livello, "impMappazzaB");
				int bitdepth = 24;
				newStackOUT = ImageStack.create(width, height, imp10.getImageStackSize(), bitdepth);
				impMappazzaOUT = new ImagePlus("MAPPAZZA_" + livello + "_" + myColors, newStackOUT);

				generate = false;
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
			// int mode = 0;
			// boolean pitturaPixel = false;
			// boolean stampa = true;
			ArrayList<Integer> pixListSignal11 = new ArrayList<Integer>();

			// ===========================================================
			// ===========================================================
			// ---- NUOVA SOLUZIONE NUMERO DUE
			// ---- ricerca posizione x,y,z del massimo hotspot NxNxN
			// ===========================================================
			// ===========================================================
			double[] mediaHotspot = new double[imp10.getImageStackSize()];
			int[] indiceHotspot = new int[imp10.getImageStackSize()];
			double maxHotspot = -99999;
			int xCenter = 0;
			int yCenter = 0;
			int indice = 0;

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {
				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				if (imp20 == null)
					continue;
				double[] pos20 = hotspotSearch(imp20, latoHotCube, timeout);
				if (pos20 == null) {
					continue;
				}

				int xRoi = (int) (pos20[0] - latoHotCube / 2);
				int yRoi = (int) (pos20[1] - latoHotCube / 2);
				ImagePlus imp21 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				imp21.setRoi(xRoi, yRoi, latoHotCube, latoHotCube);
				ImageStatistics stat21 = imp21.getStatistics();
				indiceHotspot[i1] = i1;
				mediaHotspot[i1] = stat21.mean;
				if (mediaHotspot[i1] > maxHotspot) {
					maxHotspot = mediaHotspot[i1];
					indice = i1 + 1;
					xCenter = xRoi + latoHotCube / 2;
					yCenter = yRoi + latoHotCube / 2;
				}
				imp20.close();
				imp21.close();
			}

			if (debug) {
				IJ.log("Posizione dell'hotspot piu' alto:  X= " + xCenter + " Y= " + yCenter + " Z= " + indice
						+ " media= " + mediaHotspot[indice]);
			}
			// --------------------------------------------
			// vettorizazione pixels degli hotspot NxNxN
			// ---------------------------------------------
			int pip = (latoHotCube - 1) / 2;
			for (int i1 = indice - pip; i1 < indice + pip; i1++) {
				double xCenterRoi = xCenter;
				double yCenterRoi = yCenter;
				ImagePlus imp21 = MyStackUtils.imageFromStack(imp10, i1);
				pixVectorize(imp21, xCenterRoi, yCenterRoi, latoHotCube, pixListSignal11);
				imp21.close();
			}

			// ================================================================
			if (debug) {
				IJ.log("----------- segnali di hotSpotCube  [ " + pixListSignal11.size() + " ] -----------");
				String logRiga = "";
				for (int j1 = 0; j1 < pixListSignal11.size() / 25; j1++) {
					logRiga = "";
					for (int i2 = 0; i2 < 25; i2++) {
						logRiga += pixListSignal11.get(j1 + i2) + ",  ";
					}
					IJ.log(logRiga);
				}
			}
			// }
			// ===========================================================
			// ===========================================================
			// ===========================================================
			// =============================================================

			int[] pixListSignal = ArrayUtils.arrayListToArrayInt(pixListSignal11);
			double mean11 = UtilAyv.vetMean(pixListSignal);
			if (debug)
				IJ.log("media di hotSpotCube= " + mean11);

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {

				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);

				if (debug) {
					if (i1 == debugZZ) {
						puntatore = debugYY * width + debugXX;

						short[] pixelsDebug = (short[]) imp20.getProcessor().getPixels();
						IJ.log("debug XX= " + debugXX + " YY= " + debugYY + " ZZ = " + debugZZ + "puntatore= "
								+ puntatore + " mean11= " + mean11 + " valorePixel= " + pixelsDebug[puntatore]);

					}

				}

				mappazzaGrigio16(mean11, imp20, impMappazzaR, impMappazzaG, impMappazzaB, i1 + 1, livello, minimi,
						massimi, colorCoil, myColors);

				if (!impMappazzaR.isVisible())
					impMappazzaR.show();
				if (!impMappazzaG.isVisible())
					impMappazzaG.show();
				if (!impMappazzaB.isVisible())
					impMappazzaB.show();
			}

			// IJ.log("#########################################################");

			generaMappazzaCombinata(width, height, 1, livello, impMappazzaR, impMappazzaG, impMappazzaB, impMappazzaOUT,
					myColors);
			if (!vedo) {
				impMappazzaOUT.show();
				vedo = true;
			} else
				impMappazzaOUT.updateAndRepaintWindow();

			if (!tutte)
				MyLog.waitHere();

		}

		MyLog.waitHere("FINE LAVORO");

	} // chiude

	/***
	 * Aggiunge i pixel appartenenti alla ROI all'ArrayList pixList11
	 * 
	 * @param imp11
	 * @param xCenterMROI
	 * @param yCenterMROI
	 * @param latoMROI
	 * @param pixList11
	 * @param verify
	 */
	public static void pixVectorize(ImagePlus imp11, double xCenterMROI, double yCenterMROI, double latoMROI,
			ArrayList<Integer> pixList11) {

		if (imp11 == null)
			MyLog.waitHere("imp11==null");
		if (pixList11 == null)
			MyLog.waitHere("pixList1==null");

		imp11.setRoi((int) Math.round((xCenterMROI - latoMROI / 2)), (int) Math.round(yCenterMROI - latoMROI / 2),
				(int) latoMROI, (int) latoMROI);
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
					if (debug)
						ip11.putPixel(x, y, 0);
				}

			}
		}
		// if (verify) {
		// ip11.drawRoi(roi11);
		// roi11.setFillColor(Color.green);
		// imp11.show();
		// MyLog.waitHere();
		// }
	}

	/***
	 * Ricerca delle coordinate centro area NxN con la media max
	 * 
	 * @param imp11
	 * @param info1
	 * @param mode
	 * @param timeout
	 * @return coordinate centro
	 */

	public static double[] hotspotSearch(ImagePlus imp11, int lato, int timeout) {
		ImageWindow iw11 = null;
		if (imp11 == null)
			MyLog.waitHere("imp11==null");
		if (debug) {
			// imp11.show();
			// iw11 = imp11.getWindow();
		}

		double[] out10 = MyFilter.maxPositionGeneric(imp11, lato);
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
		// if (debug)
		// IJ.wait(timeout);
		// if (iw11 == null) {
		// } else
		// iw11.close();
		imp11.close();

		return out10;
	}

	/***
	 * Genera immagine di output vuota
	 * 
	 * @param width
	 * @param height
	 * @param depth
	 * @param livello
	 * @return
	 */
	public static ImagePlus generaMappazzaVuota(int width, int height, int depth, int livello) {

		int bitdepth = 24;
		ImageStack newStack = ImageStack.create(width, height, depth, bitdepth);
		ImagePlus impMappazza = new ImagePlus("MAPPAZZA_" + livello, newStack);
		return impMappazza;
	}

	public static ImagePlus generaMappazzaVuota16(int width, int height, int depth, int livello, String titolo) {

		int bitdepth = 16;
		ImageStack newStack = ImageStack.create(width, height, depth, bitdepth);
		ImagePlus impMappazza = new ImagePlus(titolo + "_" + livello, newStack);
		// MyLog.waitHere("immaggine generata a " + impMappazza.getBitDepth() +
		// " bit");
		return impMappazza;
	}

	public static void mappazzaGrigio16(double mean11, ImagePlus imp1, ImagePlus impMappazzaR, ImagePlus impMappazzaG,
			ImagePlus impMappazzaB, int slice, int livello, int[] minimi, int[] massimi, int colorCoil, int myColors) {

		if (imp1 == null) {
			MyLog.waitHere("imp1==null");
			return;
		}
		if (impMappazzaR == null || impMappazzaG == null || impMappazzaB == null) {
			MyLog.waitHere("impMappazza R,G,B==null");
			return;
		}

		int width = imp1.getWidth();
		int height = imp1.getHeight();
		short[] pixels1 = UtilAyv.truePixels(imp1);
		double mean = mean11;

		short[] pixelsMappaR = (short[]) impMappazzaR.getStack().getProcessor(slice).getPixels();
		short[] pixelsMappaG = (short[]) impMappazzaG.getStack().getProcessor(slice).getPixels();
		short[] pixelsMappaB = (short[]) impMappazzaB.getStack().getProcessor(slice).getPixels();

		short pixSorgente = 0;
		int appoggioColore = 0;
		int posizioneArrayImmagine = 0;

		// stabilisco i livelli di colore per 12 livelli
		int[] myColor = new int[12];
		for (int i1 = 0; i1 < 12; i1++) {
			// myColor[i1] = i1 + 1;
			myColor[i1] = 13 - i1;

		}

		// colore per pixel piu' alti
		int colorUP = 1;
		// colore fuori dal fantoccio
		int colorOUT = 0;

		if (debug && stampa) {
			IJ.log("--- livelli di colore possibili --");
			IJ.log("colorUP= " + colorUP);
			for (int i1 = 0; i1 < 12; i1++) {
				IJ.log("classe " + i1 + "  " + myColor[i1]);
			}
			IJ.log("colorOUT= " + colorOUT);
			IJ.log("-------------------");
		}

		// i limiti delle classi stabilite da UTENT combinatoi con media hotCube
		double[] myMinimi = new double[livello];
		double[] myMassimi = new double[livello];
		for (int i1 = 0; i1 < livello; i1++) {
			myMinimi[i1] = ((100.0 + (double) minimi[i1]) / 100) * mean;
			myMassimi[i1] = ((100.0 + (double) massimi[i1]) / 100) * mean;
		}

		if (debug && stampa) {
			IJ.log("classi percentuali");
			for (int i1 = 0; i1 < livello; i1++) {
				IJ.log("classe " + i1 + " minimo= " + minimi[i1] + "%   massimo= " + massimi[i1] + "%");
			}
			IJ.log("classi limiti reali pixel");
			for (int i1 = 0; i1 < livello; i1++) {
				IJ.log("classe " + i1 + " minimo= " + myMinimi[i1] + " massimo= " + myMassimi[i1]);
			}
		}

		for (int y1 = 0; y1 < height; y1++) {
			for (int x1 = 0; x1 < width; x1++) {
				boolean cerca = true;
				posizioneArrayImmagine = y1 * width + x1;
				pixSorgente = pixels1[posizioneArrayImmagine];

				for (int i1 = 0; i1 < livello; i1++) {
					if (cerca && (pixSorgente > myMassimi[i1])) {
						appoggioColore = colorUP;
						cerca = false;
					}
					if (cerca && (pixSorgente > myMinimi[i1]) && (pixSorgente <= myMassimi[i1])) {
						appoggioColore = myColor[i1];
						cerca = false;
					}
				}
				if (cerca) {
					appoggioColore = colorOUT;
					cerca = false;
				}

				if (myColors == 3) {

					switch (colorCoil) {
					case 1:

						if (appoggioColore > pixelsMappaR[posizioneArrayImmagine])

							pixelsMappaR[posizioneArrayImmagine] = (short) appoggioColore;

						if (debug && (puntatore == posizioneArrayImmagine)) {
							IJ.log("inMappazzaGrigio16 pixSorgente= " + pixSorgente + " mappaR= "
									+ pixelsMappaR[posizioneArrayImmagine]);
						}
						break;
					case 2:
						if (appoggioColore > pixelsMappaG[posizioneArrayImmagine])
							pixelsMappaG[posizioneArrayImmagine] = (short) appoggioColore;
						break;
					case 3:
						if (appoggioColore > pixelsMappaB[posizioneArrayImmagine])
							pixelsMappaB[posizioneArrayImmagine] = (short) appoggioColore;
						break;
					default:
						MyLog.waitHere("GULP");
						break;

					}

				} else {

					switch (colorCoil) {
					case 1:

						pixelsMappaR[posizioneArrayImmagine] += (short) appoggioColore;
						if (debug && (puntatore == posizioneArrayImmagine)) {
							IJ.log("inMappazzaGrigio16 pixSorgente= " + pixSorgente + " mappaR= "
									+ pixelsMappaR[posizioneArrayImmagine]);
						}
						break;
					case 2:
						pixelsMappaG[posizioneArrayImmagine] += (short) appoggioColore;
						break;
					case 3:
						pixelsMappaB[posizioneArrayImmagine] += (short) appoggioColore;
						break;
					default:
						MyLog.waitHere("GULP");
						break;

					}
				}
			}
		}
		stampa = false;
		return;

	}

	public static void generaMappazzaCombinata(int width, int height, int slice, int livello, ImagePlus impMappazzaR,
			ImagePlus impMappazzaG, ImagePlus impMappazzaB, ImagePlus impMappazzaOUT, int myColors) {

		double auxR = 0;
		double auxG = 0;
		double auxB = 0;

		int[] pixelsMappazzaOUT = null;
		short[] pixelsMappaR = null;
		short[] pixelsMappaG = null;
		short[] pixelsMappaB = null;
		short largestValue = Short.MIN_VALUE;
		short largestR = Short.MIN_VALUE;
		short largestG = Short.MIN_VALUE;
		short largestB = Short.MIN_VALUE;
		short[] searchMax = new short[4];

		for (int i10 = 0; i10 < impMappazzaR.getNSlices(); i10++) {
			pixelsMappazzaOUT = (int[]) impMappazzaOUT.getStack().getPixels(i10 + 1);
			pixelsMappaR = (short[]) impMappazzaR.getStack().getPixels(i10 + 1);
			pixelsMappaG = (short[]) impMappazzaG.getStack().getPixels(i10 + 1);
			pixelsMappaB = (short[]) impMappazzaB.getStack().getPixels(i10 + 1);
			largestR = UtilAyv.vetMax(pixelsMappaR);
			largestG = UtilAyv.vetMax(pixelsMappaG);
			largestB = UtilAyv.vetMax(pixelsMappaB);
			if (largestR > searchMax[0])
				searchMax[0] = largestR;
			if (largestG > searchMax[1])
				searchMax[1] = largestG;
			if (largestB > searchMax[2])
				searchMax[2] = largestB;
			if (largestValue > searchMax[3])
				searchMax[3] = largestValue;
			largestValue = UtilAyv.vetMax(searchMax);
		}

		largestR = searchMax[0];
		largestG = searchMax[1];
		largestB = searchMax[2];
		largestValue = searchMax[3];

		double kappa = 255 / largestValue;
		double kappaR = 0;
		double kappaG = 0;
		double kappaB = 0;

		switch (myColors) {
		case 1:
			if (largestR == 0)
				largestR = 1;
			kappaR = 255 / largestR;
			if (largestG == 0)
				largestG = 1;
			kappaG = 255 / largestG;
			if (largestB == 0)
				largestB = 1;
			kappaB = 255 / largestB;
			break;
		case 2:
			kappaR = kappa;
			kappaG = kappa;
			kappaB = kappa;
			break;
		case 3:
			if (largestR == 0)
				largestR = 1;
			kappaR = 255 / largestR;
			if (largestG == 0)
				largestG = 1;
			kappaG = 255 / largestG;
			if (largestB == 0)
				largestB = 1;
			kappaB = 255 / largestB;
			break;
		}

		if (debug) {
			IJ.log("generaMappazzaCombinata >> largestR= " + largestR);
			IJ.log("generaMappazzaCombinata >> largestG= " + largestG);
			IJ.log("generaMappazzaCombinata >> largestB= " + largestB);
			IJ.log("generaMappazzaCombinata >> largestValue= " + largestValue);
			IJ.log("generaMappazzaCombinata >> kappa= " + kappa);
		}

		for (int i10 = 0; i10 < impMappazzaR.getNSlices(); i10++) {
			pixelsMappazzaOUT = (int[]) impMappazzaOUT.getStack().getPixels(i10 + 1);
			pixelsMappaR = (short[]) impMappazzaR.getStack().getPixels(i10 + 1);
			pixelsMappaG = (short[]) impMappazzaG.getStack().getPixels(i10 + 1);
			pixelsMappaB = (short[]) impMappazzaB.getStack().getPixels(i10 + 1);

			int colorRGB = 0;
			int red = 0;
			int green = 0;
			int blue = 0;

			for (int i1 = 0; i1 < pixelsMappaR.length; i1++) {

				auxR = (double) pixelsMappaR[i1] * kappaR;
				red = (int) auxR;
				auxG = (double) pixelsMappaG[i1] * kappaG;
				green = (int) auxG;
				auxB = (double) pixelsMappaB[i1] * kappaB;
				blue = (int) auxB;

				colorRGB = ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
				pixelsMappazzaOUT[i1] = colorRGB;
				if (debug && (puntatore == i1)) {
					IJ.log("pixelsMappaR= " + pixelsMappaR[i1] + " kappa= " + kappa + " auxR= " + auxR + " colorRGB= "
							+ colorRGB);
				}
			}
			impMappazzaOUT.updateAndRepaintWindow();
		}
		return;
	}

} // ultima
