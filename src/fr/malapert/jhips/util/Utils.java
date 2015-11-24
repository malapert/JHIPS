/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.malapert.jhips.util;

/**
 *
 * @author Jean-Christophe Malapert
 */
public class Utils {

    public static final String anim = "|/-\\";

    public static void monitoring(long current, long total) {
        int percent = (int) (current * 100 / total);
        if (percent % 10 == 0) {
            System.out.print("\r" + anim.charAt((int) current % anim.length()) + " " + percent + "% done");
            System.out.flush();
        }
    }
    
    public static void monitoringFile(String filename, long current, long total) {
        int percent = (int) (current * 100 / total);
        if (percent % 10 == 0) {
            System.out.print("\r Processing " + filename + " "+anim.charAt((int) current % anim.length()) + " " + percent + "% done");
            System.out.flush();
        }
    }    

}
