package uni3D;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import utils.AboutBox;
import utils.ArrayUtils;
import utils.MyConst;
import utils.MyLog;
import utils.MyStackUtils;
import utils.MyVersionUtils;
import utils.ReadDicom;
import utils.UtilAyv;

//=====================================================
//     Programma per plot 3D per immagini uncombined circolari
//     1 settembre 2016 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class UncombinedStackComposer_ implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = true;

	public void run(String arg) {

		// new MyAboutBox().about10("Unifor3D");
		new AboutBox().about("Uncombined3D", MyVersionUtils.CURRENT_VERSION);
		IJ.wait(2000);
		new AboutBox().close();

		// double maxFitError = +20;
		// double maxBubbleGapLimit = 2;
		IJ.log("- start -");
		UtilAyv.logResizer();

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

		do {

			String dir10 = Prefs.get("prefer.Uncombined3D_dir1", "none");
			DirectoryChooser.setDefaultDirectory(dir10);
			DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA IMMAGINI");
			String dir1 = od1.getDirectory();
			if (dir1 == null)
				return;
			Prefs.set("prefer.Uncombine3D_dir1", dir1);
			DirectoryChooser.setDefaultDirectory(dir10);
			// ------------------------------
			String[] dir1a = new File(dir1).list();
			String[] dir1b = new String[dir1a.length];
			for (int i1 = 0; i1 < dir1a.length; i1++) {
				dir1b[i1] = dir1 + "\\" + dir1a[i1];
			}
			String[][] sortedList1 = pathSorterUncombined(dir1b);
			// ------------------------------
			// ResultsTable rt3 = vectorResultsTable2(sortedList1);
			// rt3.show("sortedList1");
			// MyLog.waitHere("verificare sortedlist");
			// ------------------------------

			String[][] vetConta = contaList(sortedList1);

			// ResultsTable rt14 = vectorResultsTable2(vetConta);
			// rt14.show("VetConta");
			// MyLog.waitHere("verificare vetConta dopo contaList");

			createDirectory(dir1 + "\\stack\\");

			for (int k1 = 0; k1 < vetConta[0].length; k1++) {
				IJ.log("salvo stack " + (k1 + 1) + " / " + vetConta[0].length);
				int coil1 = k1;
				String coil2 = vetConta[0][k1];
				int num1 = Integer.valueOf(vetConta[1][k1]);
				String[] dir1c = estrai(sortedList1, coil1, num1);
				// ResultsTable rt4 = vectorResultsTable(dir1c);
				// rt4.show("dir1c");
				// MyLog.waitHere("coil2= " + coil2);
				ImagePlus imp10 = MyStackUtils.imagesToStack16(dir1c);
				new FileSaver(imp10).saveAsTiff(dir1 + "\\stack\\" + coil2);
				imp10.close();
			}

			MyLog.waitHere("FINITO SALVATAGGIO STACKS UNCOMBINED");
		} while (true);

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

	public static String[] estrai(String[][] list1, int coil1, int num1) {

		String[] list2 = new String[num1];
		int aux1 = coil1 * num1;
		for (int w1 = 0; w1 < num1; w1++) {
			list2[w1] = list1[0][aux1 + w1];
		}
		return list2;
	}

	public static String[][] contaList(String[][] mat1) {
		List<Integer> list1 = new ArrayList<Integer>();
		List<String> list2 = new ArrayList<String>();
		String old = "";
		int conta = 0;

		// ResultsTable rt4 = vectorResultsTable2(mat1);
		// rt4.show("input contaList");
		// MyLog.waitHere("input a contaList");

		String aux1 = null;
		for (int i1 = 0; i1 < mat1[0].length; i1++) {
			aux1 = mat1[1][i1];
			if (i1 == 0)
				old = aux1;
			if (old.equals(aux1)) {
				conta++;
			} else {
				list1.add(conta);
				list2.add(old);
				conta = 1;
				old = aux1;
			}
		}
		list1.add(conta);
		list2.add(aux1);

		String[] vetCoil = ArrayUtils.arrayListToArrayString(list2);
		// ResultsTable rt44 = vectorResultsTable(vetCoil);
		// rt44.show("vetCoil");
		// MyLog.waitHere("verificare vetCoil dentro contaList");

		int[] vetConta = ArrayUtils.arrayListToArrayInt(list1);
		String[][] vetOut = new String[2][vetCoil.length];

		for (int i1 = 0; i1 < vetCoil.length; i1++) {
			vetOut[0][i1] = vetCoil[i1];
			vetOut[1][i1] = "" + vetConta[i1];
		}

		// ResultsTable rt5 = vectorResultsTable2(vetOut);
		// rt4.show("vetOut contaList");
		// MyLog.waitHere();

		return vetOut;
	}

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
			IJ.showProgress(w1, path.length);
			IJ.log("apro immagine " + w1 + " / " + path.length);
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
		// ResultsTable rt3 = vectorResultsTable2(matStr0);
		// rt3.show("INIZIALE");
		// MyLog.waitHere();

		String[][] matStr2 = insertionSort(matStr0, 2, "sortPOSIZIONE");
		// MyLog.logMatrixDimensions(matStr2, "matStr0");
		//
		// ResultsTable rt5 = vectorResultsTable2(matStr2);
		// rt5.show("Sortato per POSIZIONE");
		// MyLog.waitHere();

		// MyLog.logMatrixDimensions(matStr0, "matStr0");
		String[][] matStr1 = insertionSort(matStr2, 1, "sortBOBINA");

		// ResultsTable rt4 = vectorResultsTable2(matStr1);
		// rt4.show("Sortato per BOBINA");
		// MyLog.waitHere();

		return matStr1;
	}

	/***
	 * Insertion sort è un algoritmo stabile, quindi utilizzabile per sort a
	 * criteri multipli
	 * 
	 * @param tableIn
	 * @param key
	 * @param titolo
	 * @return
	 */

	public static String[][] insertionSort(String[][] tableIn, int key, String titolo) {

		// ResultsTable rtxx = Uncombined3D_.vectorResultsTable2(tableIn);
		// rtxx.show("input a insertionSort" + key);
		// MyLog.waitHere();

		String[][] tableOut = duplicateTable(tableIn);
		String[] vetKey = new String[tableIn[0].length];
		int[] vetIndex = new int[tableIn[0].length];
		for (int i1 = 0; i1 < tableOut[0].length; i1++) {
			String strKey = getKey(tableOut, i1, key);
			vetKey[i1] = strKey;
			vetIndex[i1] = i1;
		}
		// effettuo insertionSort su key, gli altri campi andranno in parallelo
		boolean numeric = true;
		for (int i1 = 0; i1 < vetKey.length; i1++) {
			try {
				Double.parseDouble(vetKey[i1]);
			} catch (NumberFormatException e) {
				numeric = false;
			}
		}

		if (numeric) {
			double[] vetKeyN = new double[vetKey.length];
			for (int i1 = 0; i1 < vetKey.length; i1++) {
				vetKeyN[i1] = Double.parseDouble(vetKey[i1]);
			}

			for (int i1 = 1; i1 < vetKeyN.length; i1++) {
				double tmp1 = vetKeyN[i1];
				int tmp2 = vetIndex[i1];
				int j1 = i1 - 1;
				for (j1 = i1 - 1; (j1 >= 0) && (vetKeyN[j1] > tmp1); j1--) {
					vetKeyN[j1 + 1] = vetKeyN[j1];
					vetIndex[j1 + 1] = vetIndex[j1];
				}
				vetKeyN[j1 + 1] = tmp1;
				vetIndex[j1 + 1] = tmp2;
			}
			// ResultsTable rt1 = Uncombined3D_.vectorResultsTable2(vetKeyN,
			// vetIndex);
			// rt1.show("vetKey Double sorted");
			// MyLog.waitHere();

		} else {

			for (int i1 = 1; i1 < vetKey.length; i1++) {
				String tmp1 = vetKey[i1];
				int tmp2 = vetIndex[i1];
				int j1 = i1 - 1;
				for (j1 = i1 - 1; (j1 >= 0) && (vetKey[j1].compareTo(tmp1)) > 0; j1--) {
					vetKey[j1 + 1] = vetKey[j1];
					vetIndex[j1 + 1] = vetIndex[j1];
				}
				vetKey[j1 + 1] = tmp1;
				vetIndex[j1 + 1] = tmp2;
			}

			// ResultsTable rt1 = Uncombined3D_.vectorResultsTable2(vetKey,
			// vetIndex);
			// rt1.show("vetKey String sorted");
			// MyLog.waitHere();
		}

		for (int i2 = 0; i2 < tableOut[0].length; i2++) {
			tableOut[0][i2] = tableIn[0][vetIndex[i2]];
			tableOut[1][i2] = tableIn[1][vetIndex[i2]];
			tableOut[2][i2] = tableIn[2][vetIndex[i2]];
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

	public static ResultsTable vectorResultsTable(String[] vetClassi) {

		ResultsTable rt1 = ResultsTable.getResultsTable();
		String t1 = "Numerosita' Classi";
		for (int i1 = 0; i1 < vetClassi.length; i1++) {
			rt1.incrementCounter();
			rt1.addValue(t1, vetClassi[i1]);
		}
		return rt1;
	}

} // ultima
