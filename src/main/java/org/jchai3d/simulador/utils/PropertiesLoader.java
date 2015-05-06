/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jchai3d.simulador.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author T315443
 */
public class PropertiesLoader {

    public static PropertiesLoader propertiesLoader;
    private Properties props;
    private String nomeDoProperties = null;

    public static PropertiesLoader getInstance(String nomeDoProperties) {
        if (propertiesLoader == null) {
            propertiesLoader = new PropertiesLoader(nomeDoProperties);
        }

        return propertiesLoader;
    }

    private PropertiesLoader(String nomeDoProperties) {

        this.nomeDoProperties = nomeDoProperties;

        props = new Properties();
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(nomeDoProperties);
        try {
            props.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getValor(String chave) {
        return (String) props.getProperty(chave);
    }
}
