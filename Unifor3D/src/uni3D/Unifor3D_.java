package uni3D;

import java.awt.Frame;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import utils.ArrayUtils;
import utils.InputOutput;
import utils.MyConst;
import utils.MyStackUtils;
import utils.ReadDicom;
import utils.UtilAyv;
import ij.*;
import ij.io.*;
import ij.plugin.*;
import ij.text.TextWindow;

//=====================================================
//     Programma per uniformita' 3D per immagini combined circolari
//     25 giugno 2004 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class Unifor3D_ implements PlugIn {

	public void run(String arg) {

		boolean nuovo1;

		nuovo1 = true;

		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}

		TextWindow tw = new TextWindow("Sequenze", "<-- INIZIO Sequenze -->", 300, 200);
		Frame lw = WindowManager.getFrame("Sequenze");
		if (lw == null)
			return;
		lw.setLocation(10, 10);

		// chiede di selezionare manualmente le cartelle con le
		// immagini Si suppone che le immagini siano trasferite utilizzando un
		// hard-disk OPPURE che venga fatto un DicomSorter.
		// QUINDI: IL PROGRAMMA SI FIDA DELL'OPERATORE

		if (nuovo1 == true) {
			DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA PRIMA ACQUISIZIONE");
			String dir1 = od1.getDirectory();
			String[] dir1a = new File(dir1).list();
			int num1 = dir1a.length;
			String[] sortedList1 = pathSorter(dir1a);
			ImagePlus imp10 = MyStackUtils.imagesToStack16(sortedList1);
			UtilAyv.showImageMaximized2(imp10);


			DirectoryChooser od2 = new DirectoryChooser("SELEZIONARE CARTELLA SECONDA ACQUISIZIONE");
			String dir2 = od2.getDirectory();
			String[] dir2a = new File(dir2).list();
			int num2 = dir2a.length;
			String[] sortedList2 = pathSorter(dir2a);
			ImagePlus imp20 = MyStackUtils.imagesToStack16(sortedList2);
			UtilAyv.showImageMaximized2(imp20);
			
			IJ.showMessage("FINE LAVORO");
		}
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
				IJ.log("pathSorter: image file unavailable?");
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

} // ultima
