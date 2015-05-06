/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jchai3d.simulador;

import org.jchai3d.display.JViewport;
import org.jchai3d.math.JMaths;
import org.jchai3d.math.JVector3d;
import org.jchai3d.scenegraph.JCamera;
import org.jchai3d.scenegraph.JGenericObject;
import org.jchai3d.scenegraph.JMesh;
import org.jchai3d.scenegraph.JWorld;
import org.jchai3d.sim.builder.SimulationRuntime;

/**
 *
 * @author Jairo (jairossmunb@gmail.com)
 */
public class ProcessoGrafico {
    //-----------------------------------------------------------------------
    // PADRÂO DE PROJETO JCHAI3D: singleton
    //-----------------------------------------------------------------------

    public static ProcessoGrafico processoGrafico = null;
    //-----------------------------------------------------------------------
    // MEMBERS JCHAI3D:
    //-----------------------------------------------------------------------
    private JWorld world; // objeto responsavel por tratar o mundo grafico
    private JViewport viewport; // objeto reponsavel por apresentar o mundo grafico
    private JCamera camera; // objeto para tratamento da camera e relacionamento do world com a viewport
    //-----------------------------------------------------------------------
    // MEMBERS: Properties (CAMERA)
    //-----------------------------------------------------------------------
    private double cameraAngleH;
    private double cameraAngleV;
    private double cameraDistance;
    private JVector3d cameraPosition = new JVector3d();
    private JVector3d pos = new JVector3d();
    private JVector3d lookAt = new JVector3d();
    private JVector3d up = new JVector3d();
    private double near;
    private double far;
    private double ANGLE_ROTATION = 0.5;
    private double TAX_ZOOM = 1;

    //-----------------------------------------------------------------------
    // METHODS:
    //-----------------------------------------------------------------------
    public JViewport getViewport() {
        return viewport;
    }

    public JWorld getWorld() {
        return world;
    }

    public JCamera getCamera() {
        return camera;
    }

    public double getCameraAngleH() {
        return cameraAngleH;
    }

    public double getCameraAngleV() {
        return cameraAngleV;
    }

    public double getCameraDistance() {
        return cameraDistance;
    }

    public void setCameraAngleH(double cameraAngleH) {
        this.cameraAngleH = cameraAngleH;
    }

    public void setCameraAngleV(double cameraAngleV) {
        this.cameraAngleV = cameraAngleV;
    }

    public void setCameraDistance(double cameraDistance) {
        this.cameraDistance = cameraDistance;
    }

    public JVector3d getPos() {
        return pos;
    }

    public void setPos(JVector3d pos) {
        this.pos = pos;
    }

    public JVector3d getLookAt() {
        return lookAt;
    }

    public void setLookAt(JVector3d lookAt) {
        this.lookAt = lookAt;
    }

    public JVector3d getUp() {
        return up;
    }

    public void setUp(JVector3d up) {
        this.up = up;
    }

    public double getNear() {
        return near;
    }

    public void setNear(double near) {
        this.near = near;
    }

    public double getFar() {
        return far;
    }

    public void setFar(double far) {
        this.far = far;
    }

    //-----------------------------------------------------------------------
    // CONSTRUCTOR:
    //-----------------------------------------------------------------------
    private ProcessoGrafico(SimulationRuntime simulationRuntime) {

        // Pass 1
        createWorld(simulationRuntime);

        // pass 2
        createCamera(simulationRuntime);

        // Pass 3
        createViewport(simulationRuntime);

    }

    /**
     * 
     * @return 
     */
    public static ProcessoGrafico getInstance(SimulationRuntime simulationRuntime) {
        if (processoGrafico == null) {
            processoGrafico = new ProcessoGrafico(simulationRuntime);
        }

        return processoGrafico;
    }

    private void createWorld(SimulationRuntime simulationRuntime) {

        world = simulationRuntime.getWorld();

    }

    private void createViewport(SimulationRuntime simulationRuntime) {

        viewport = simulationRuntime.getViewport();
    }

    private void createCamera(SimulationRuntime simulationRuntime) {
        camera = simulationRuntime.getCamera();

        updateCameraPosition();
    }

    public void updateCameraPosition() {
        // check values
        if (getCameraDistance() < 0.1) {
            setCameraDistance(0.1);
        }
        if (getCameraAngleV() > 89) {
            setCameraAngleV(89);
        }
        if (getCameraAngleV() < -89) {
            setCameraAngleV(-89);
        }

        // compute position of camera in space
        JVector3d pos =
                JMaths.jAdd(
                cameraPosition,
                new JVector3d(
                getCameraDistance() * JMaths.jCosDeg(getCameraAngleH()) * JMaths.jCosDeg(getCameraAngleV()),
                getCameraDistance() * JMaths.jSinDeg(getCameraAngleH()) * JMaths.jCosDeg(getCameraAngleV()),
                getCameraDistance() * JMaths.jSinDeg(getCameraAngleV())));

        // compute lookat position
        JVector3d lookat = cameraPosition;

        // define role orientation of camera
        JVector3d up = new JVector3d(0.0, 0.0, 1.0);

        // set new position to camera
        camera.set(pos, lookat, up);

        // recompute global positions
        world.computeGlobalPositions(true, new JVector3d(0.0, 0.0, 0.0), JMaths.jIdentity3d());
    }

    /**
     * Método responsável por rotacional o objeto sobre o mesmo eixo
     * 
     * @param xold
     * @param yold
     * @param x
     * @param y 
     */
    void rotateObject(int xold, int yold, int x, int y) {

        int numChildren = getWorld().getNumChildren();
        JVector3d pos = new JVector3d(0.0, 0.0, 0.0);

        for (int index = 0; index < numChildren; index++) {
            JGenericObject jGenericObject = getWorld().getChild(index);

            if (jGenericObject instanceof JMesh) {
                if (xold != x) {
                    if (xold > x) {
                        pos.setZ(1.0);
                        pos.setY(0.0);
                        jGenericObject.rotate(pos, ANGLE_ROTATION);
                    } else {
                        pos.setZ(1.0);
                        pos.setY(0.0);
                        jGenericObject.rotate(pos, -ANGLE_ROTATION);
                    }
                }

                if (yold != y) {
                    if (yold > y) {
                        pos.setZ(0.0);
                        pos.setY(1.0);
                        jGenericObject.rotate(pos, ANGLE_ROTATION);
                    } else {
                        pos.setZ(0.0);
                        pos.setY(1.0);
                        jGenericObject.rotate(pos, -ANGLE_ROTATION);
                    }
                }
            }
        }

    }

    /**
     * Método responsável por aumentar o diminuir a distancia da camera em relação
     * ao objeto
     * 
     * @param oldy
     * @param y 
     */
    void zoomCamera(int oldy, int y) {
        setCameraDistance(getCameraDistance() - (TAX_ZOOM * (y - oldy)));
    }
}
