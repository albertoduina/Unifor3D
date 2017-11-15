package uni3D;

import static org.junit.Assert.assertTrue;

import java.awt.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.measure.ResultsTable;
import utils.InputOutput;
import utils.MyCircleDetector;
import utils.MyFilter;
import utils.MyLog;
import utils.UtilAyv;

public class Uncombined3D_Test2 {

	@Before
	public void setUp() throws Exception {
		new ImageJ(ImageJ.NORMAL);
	}

	@After
	public void tearDown() throws Exception {
		// new WaitForUserDialog("Do something, then click OK.").show();

	}

	
	@Test
	public final void testPositionSearchZZ() {

		boolean demo = false;
		double profond = 30;
		String info1 = "INFO1";
		int timeout = 30000;
		int mode = 3;

		String path1 = "./Data/DUP_XZ_128.tif";
		ImagePlus imp13 = UtilAyv.openImageNoDisplay(path1, demo);

		double[] circleData = Uncombined3D_.positionSearchZZ(imp13, profond, info1, mode, timeout);
		imp13.close();

		ImagePlus imp14 = UtilAyv.openImageMaximized(path1);
		Overlay over14 = new Overlay();
		imp14.setOverlay(over14);

		double xCenterRoi = circleData[0];
		double yCenterRoi = circleData[1];
		double xCenterCircle = circleData[2];
		double yCenterCircle = circleData[3];
		double xMaxima = circleData[4];
		double yMaxima = circleData[5];
		double angle = circleData[6];
		double xBordo = circleData[7];
		double yBordo = circleData[8];
		double diamCircle = circleData[9];
		double diamRoi = 11;
		double diamMaxima = 11;

		// MyCircleDetector.drawCenter(imp14, over14, xCenterCircle,
		// yCenterCircle, Color.green);
		MyCircleDetector.drawCenter(imp14, over14, xCenterRoi, yCenterRoi, Color.green);
		// MyCircleDetector.drawCenter(imp14, over14, xMaxima, yMaxima,
		// Color.red);

		// int aux1 = (int) Math.round(diamCircle / 2.0);
		// imp14.setRoi(new OvalRoi(xCenterCircle - aux1, yCenterCircle - aux1,
		// diamCircle, diamCircle));
		// imp14.getRoi().setStrokeColor(Color.green);
		// over14.addElement(imp14.getRoi());
		// imp14.deleteRoi();
		// imp14.updateAndDraw();

		// imp14.setRoi(new OvalRoi(xCenterRoi - diamRoi / 2, yCenterRoi -
		// diamRoi / 2, diamRoi, diamRoi));
		// imp14.setRoi(new OvalRoi(xCenterRoi, yCenterRoi, diamRoi, diamRoi));
		imp14.setRoi((int) Math.round(xCenterRoi - diamRoi / 2), (int) Math.round(yCenterRoi - diamRoi / 2),
				(int) diamRoi, (int) diamRoi);

		imp14.getRoi().setStrokeColor(Color.green);
		over14.addElement(imp14.getRoi());
		imp14.deleteRoi();
		imp14.updateAndDraw();

		// int aux2 = (int) Math.round(diamMaxima / 2.0);
		// imp14.setRoi((int) Math.round(xMaxima - diamMaxima / 2), (int)
		// Math.round(yMaxima - diamMaxima / 2),
		// (int) diamMaxima, (int) diamMaxima);
		// imp14.getRoi().setStrokeColor(Color.red);
		// over14.addElement(imp14.getRoi());
		// imp14.deleteRoi();
		// imp14.updateAndDraw();
		MyLog.waitHere();

	}

	@Test
	public final void testPositionSearchCircular() {

		String path1 = "./Data/DUP_XZ_128.tif";
		ImagePlus imp1 = UtilAyv.openImageMaximized(path1);
		int xpos = 135;
		int ypos = 89;
		int diam = 169;
		double[] circleData = new double[3];
		circleData[0] = (double) xpos;
		circleData[1] = (double) ypos;
		circleData[2] = (double) diam;
		double diam2 = 13;
		int demolevel = 3;
		double[] out = MyFilter.positionSearchCircular1(imp1, circleData, diam2, demolevel);
		MyLog.waitHere();
	}
}
