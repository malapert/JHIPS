/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.malapert.jhips.algorithm;

import fr.malapert.jhips.exception.ProjectionException;
import healpix.essentials.Pointing;
import healpix.essentials.Vec3;
import javax.xml.bind.PropertyException;

/**
 *
 * @author Jean-Christophe Malapert
 */
public class Projection {

    public enum ProjectionType {

        CAR, TAN
    };

    private static double[] unProjectTan(double[] centerCameraPixels, double[] cameraHori, double[] scale, double[] rotation, double azimuth, double elevation) throws ProjectionException {
        Pointing pt = new Pointing(0.5*Math.PI - elevation, azimuth);
        Pointing ptCenter = new Pointing(0.5*Math.PI - cameraHori[1], cameraHori[0]);
        Vec3 vecPt = new Vec3(pt);
        Vec3 vecCenter = new Vec3(ptCenter);
        double angle  = vecPt.angle(vecCenter);

        if (angle > 0.5 * Math.PI) {
            throw new ProjectionException();
        }

        // computes the scale and rotation matrix
        double cd11 = scale[0] * Math.cos(rotation[1]);
        double cd12 = Math.abs(scale[1]) * scale[0] / Math.abs(scale[0]) * Math.sin(rotation[1]);
        double cd21 = -1 * Math.abs(scale[0]) * scale[1] / Math.abs(scale[1]) * Math.sin(rotation[1]);
        double cd22 = scale[1] * Math.cos(rotation[1]);

        double h = Math.sin(elevation) * Math.sin(cameraHori[1])
                + Math.cos(elevation) * Math.cos(cameraHori[1]) * Math.cos(azimuth - cameraHori[0]);

        double dAz = Math.cos(elevation) * Math.sin(azimuth - cameraHori[0]) / h;
        double dEl = (Math.sin(elevation) * Math.cos(cameraHori[1])
                - Math.cos(elevation) * Math.sin(cameraHori[1]) * Math.cos(azimuth - cameraHori[0])) / h;
        double det = cd22 * cd11 - cd12 * cd21;

        double x = centerCameraPixels[0] - (cd12 * dEl - cd22 * dAz) / det;
        double y = centerCameraPixels[1] + (cd11 * dEl - cd21 * dAz) / det;

        return new double[]{x, y};

    }

    private static double[] unProjectCAR(double[] centerCameraPixels, double[] cameraHori, double[] scale, double[] rotation, double azimuth, double elevation) {
        // Find the angular distance between the pt and camera's center projected on each axis (azimuth, elevation)
        double variationLongitude = azimuth - cameraHori[0];
        if (variationLongitude > Math.PI) {
            variationLongitude = -2 * Math.PI + variationLongitude;
        }

        double variationLatitude = elevation - cameraHori[1];
        double[] variationCoord = new double[]{variationLongitude, variationLatitude};

        // Find the number of pixel to move from the camera's center
        double[] pixelToMoveAlongXY = new double[]{variationCoord[0] / scale[0], variationCoord[1] / scale[1]};

        int x = (int) (-pixelToMoveAlongXY[0] + centerCameraPixels[0]);
        int y = (int) (pixelToMoveAlongXY[1] + centerCameraPixels[1]);
        return new double[]{x, y};
    }

    public static double[] unProject(double[] cameraPixel, double[] cameraHori, double[] scale, double[] rotation, double azimuth, double elevation, ProjectionType type) throws ProjectionException {
        double[] xy;
        switch (type) {
            case TAN:
                xy = unProjectTan(cameraPixel, cameraHori, scale, rotation, azimuth, elevation);
                break;
            case CAR:
                xy = unProjectCAR(cameraPixel, cameraHori, scale, rotation, azimuth, elevation);
                break;
            default:
                throw new UnsupportedOperationException("The projection " + type.name() + " is not supported");
        }
        return xy;
    }

}
