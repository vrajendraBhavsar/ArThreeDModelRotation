package org.the3deer.android_3d_model_engine.services.wavefront;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Materials;
import org.the3deer.util.math.Math3DUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

final class WavefrontMaterialsParser {

    /*
     * Parse the MTL file line-by-line, building Material objects which are collected in the materials ArrayList.
     */
    Materials parse(String id, InputStream inputStream, float[] userSelectedObjColor) {

        Log.i("WavefrontMaterialsParse", "Parsing materials... ");

        final Materials materials = new Materials(id);
        try {

            final BufferedReader isReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            Material currMaterial = new Material(); // current material
            boolean createNewMaterial = false;

            while (((line = isReader.readLine()) != null)) {

                // read next line
                line = line.trim();

                // ignore empty lines
                if (line.length() == 0) continue;

                // parse line
                if (line.startsWith("newmtl ")) { // new material

                    // new material next iteration
                    if (createNewMaterial) {

                        // add current material to the list
                        materials.add(currMaterial.getName(), currMaterial);

                        // prepare next material
                        currMaterial = new Material();
                    }

                    // create next material next time
                    createNewMaterial = true;

                    // configure material
                    currMaterial.setName(line.substring(6).trim());

                    /**
                     * Portion of Sofa who's color needed to be changed can be caught here
                     */
                    // log event
                    Log.d("WavefrontMaterialsParse", "New material found: " + currMaterial.getName());

                } else if (line.startsWith("map_Kd ")) { // texture filename

                    // bind texture
                    currMaterial.setTextureFile(line.substring(6).trim());

                    // log event
                    Log.v("WavefrontMaterialsParse", "Texture found: " + currMaterial.getTextureFile());

                } else if (line.startsWith("Ka ")) {

                    // ambient colour
                    currMaterial.setAmbient(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    // log event
                    Log.v("WavefrontMaterialsParse", "Ambient color: " + Arrays.toString(currMaterial.getAmbient()));
                } else if (line.startsWith("Kd ")) {
                    float[] kdColor = Math3DUtils.parseFloat(line.substring(2).trim().split(" "));
                    Log.d("TAG", "!@# parse: "+Arrays.toString(kdColor));
                    // diffuse colour
                    if (Objects.equals(currMaterial.getName(), "Procedural_Simple_Cloth")) {
//                        float[] userKdColor = {0.000000f, 0.000000f, 1.000000f};
//                        float[] userKdColor = {0.46666667f, 0.7882353f, 0.87058824f};
//                        currMaterial.setDiffuse(userKdColor);
                        Log.d("?", "!@# parse: Kd ==>"+Arrays.toString(userSelectedObjColor));
                        currMaterial.setDiffuse(userSelectedObjColor);
                    } else {
                        currMaterial.setDiffuse(kdColor);
                    }

                    /**
                     * Can change color of the part of the obj from here
                     */
                    // log event
                    Log.v("WavefrontMaterialsParse", "Diffuse color: " + Arrays.toString(currMaterial.getDiffuse()));
                } else if (line.startsWith("Ks ")) {

                    // specular colour
                    currMaterial.setSpecular(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    // log event
                    Log.v("WavefrontMaterialsParse", "Specular color: " + Arrays.toString(currMaterial.getSpecular()));
                } else if (line.startsWith("Ns ")) {

                    // shininess
                    float val = Float.parseFloat(line.substring(3));
                    currMaterial.setShininess(val);

                    // log event
                    Log.v("WavefrontMaterialsParse", "Shininess: " + currMaterial.getShininess());

                } else if (line.charAt(0) == 'd') {

                    // alpha
                    float val = Float.parseFloat(line.substring(2));
                    currMaterial.setAlpha(val);

                    // log event
                    Log.v("WavefrontMaterialsParse", "Alpha: " + currMaterial.getAlpha());

                } else if (line.startsWith("Tr ")) {

                    // Transparency (inverted)
                    currMaterial.setAlpha(1 - Float.parseFloat(line.substring(3)));

                    // log event
                    Log.v("WavefrontMaterialsParse", "Transparency (1-Alpha): " + currMaterial.getAlpha());

                } else if (line.startsWith("illum ")) {

                    // illumination model
                    Log.v("WavefrontMaterialsParse", "Ignored line: " + line);

                } else if (line.charAt(0) == '#') { // comment line

                    // log comment
                    Log.v("WavefrontMaterialsParse", line);

                } else {

                    // log event
                    Log.v("WavefrontMaterialsParse", "Ignoring line: " + line);
                }

            }

            // add last processed material
            materials.add(currMaterial.getName(), currMaterial);

        } catch (Exception e) {
            Log.e("WavefrontMaterialsParse", e.getMessage(), e);
        }

        // log event
        Log.i("WavefrontMaterialsParse", "Parsed materials: "+materials);

        return materials;
    }
}
