/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jchai3d.simulador;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jchai3d.devices.JGenericHapticDevice;
import org.jchai3d.devices.JHapticDeviceHandler;
import org.jchai3d.devices.JHapticDeviceInfo;
import org.jchai3d.files.JMeshLoader;
import org.jchai3d.graphics.JMaterial;
import org.jchai3d.math.JConstants;
import org.jchai3d.math.JMaths;
import org.jchai3d.math.JVector3d;
import org.jchai3d.scenegraph.JGenericObject;
import org.jchai3d.scenegraph.JMesh;
import org.jchai3d.timers.JPrecisionClock;
import org.jchai3d.timers.JThread;
import org.jchai3d.timers.JThreadPriority;
import org.jchai3d.tools.JGeneric3dofPointer;

/**
 *
 * @author T315443
 */
public final class ProcessoHaptico implements Runnable {

    private JGeneric3dofPointer tool;
    private JHapticDeviceHandler handler;
    private JGenericHapticDevice hapticDevice;
    private JHapticDeviceInfo info;
    private JPrecisionClock simClock;
    private float proxyRadius;
    private double workspaceScaleFactor;
    private double stiffnessMax;
    private boolean enableHaptic = false;
    //padrão de projeto
    public static ProcessoHaptico processoHaptico = null;
    private ProcessoGrafico processoGrafico;
    /* configura a thread de tratamento haptico */
    private JThread threadHaptica = new JThread();

    public JThread getThreadHaptica() {
        return threadHaptica;
    }
    //-----------------------------------------------------------------------
    // CONSTRUCTOR:
    //-----------------------------------------------------------------------

    private ProcessoHaptico(ProcessoGrafico processoGrafico, boolean enableHapticDevice) {

        this.processoGrafico = processoGrafico;

        createHapticDevice(processoGrafico, enableHapticDevice);
    }

    private void createMeshHaptic() {
        JMesh drill = new JMesh(processoGrafico.getWorld());
        try {
            JMeshLoader.loadMeshFromFile(drill, new File("drill.obj"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ProcessoHaptico.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (IOException ex) {
            Logger.getLogger(ProcessoHaptico.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
                
        drill.scale(0.04, true);
        drill.setPosition(-900, 0, 0);

        drill.deleteCollisionDetector(true);
        
        JMaterial mat = new JMaterial();
        mat.getAmbient().set(0.5f, 0.5f, 0.5f, 0.5f);
        mat.getDiffuse().set(0.8f, 0.8f, 0.8f, 0.8f);
        mat.getSpecular().set(1.0f, 1.0f, 1.0f, 1.0f);

        drill.setMaterial(mat, true);
        drill.computeAllNormals(true);
        
        
        
        tool.getProxyMesh().addChild(drill);
    }

    public void createHapticDevice(ProcessoGrafico processoGrafico, boolean enableHapticDevice) {

        if (handler == null) {
            handler = new JHapticDeviceHandler();
        }
        
        handler.update();

        /* recupera a 1ª interface hatpica */
        hapticDevice = handler.getDevice(0);

        /* recupera as informações da interface haptica */
        if (hapticDevice != null) {
            info = hapticDevice.getSpecifications();
        }

        /* adiciona objeto que representa interface 3D na tela */
        if (tool == null) {
            tool = new JGeneric3dofPointer(processoGrafico.getWorld());
        }

        processoGrafico.getWorld().addChild(tool);

        /* associa objeto 3D a interface haptica */
        tool.setHapticDevice(hapticDevice);

        tool.start();

        tool.setWorkspaceRadius(1.0);

        tool.setRadius(0.01);

        tool.getDeviceSphere().setVisible(false, false);

        proxyRadius = 0.03f;

        tool.getProxyPointForceModel().setProxyRadius(proxyRadius);

        tool.getProxyPointForceModel().getCollisionSettings().setCheckBothSidesOfTriangles(false);

        tool.getProxyPointForceModel().setDynamicProxyEnabled(false);

        tool.getProxyPointForceModel().setForceShadingEnabled(true);

        workspaceScaleFactor = tool.getWorkspaceScaleFactor();

        stiffnessMax = info.mMaxForceStiffness / workspaceScaleFactor;

        createMeshHaptic();

        /* ativa o processo haptico */
        processoGrafico.getWorld().setHapticEnabled(enableHapticDevice, enableHapticDevice);

        this.enableHaptic = enableHapticDevice;

        threadHaptica.set(this, JThreadPriority.CHAI_THREAD_PRIORITY_HAPTICS);
    }

    /**
     * 
     * @return 
     */
    public static ProcessoHaptico getInstance(ProcessoGrafico processoGrafico, boolean enableHapticDevice) {
        if (processoHaptico == null) {
            processoHaptico = new ProcessoHaptico(processoGrafico, enableHapticDevice);
        }

        return processoHaptico;
    }

    @Override
    public void run() {

        // rotational velocity of the object
        JVector3d rotVel = new JVector3d(0, 0, 0);
        JVector3d rotAcc = new JVector3d(0, 0, 0);

        // a virtual object
        JMesh object = null;

        simClock = new JPrecisionClock();

        simClock.reset();

        /* verifica se o processo haptico esta habilitato */
        if (enableHaptic) {

            JThread.safeSleep(10000, "Incio da Thread em 10 segundos");

            /* tratamento da thread de renderização haptica */
            while (processoGrafico.getViewport().isStartSimulation()) {
                // compute global reference frames for each object
                processoGrafico.getWorld().computeGlobalPositions(true);

                // update position and orientation of tool
                tool.updatePose();

                // compute interaction forces
                tool.computeInteractionForces();

                // send forces to device
                tool.applyForces();

                // stop the simulation clock
                simClock.stop();

                // read the time increment in seconds
                double timeInterval = simClock.getCurrentTimeSeconds();

                // restart the simulation clock
                simClock.reset();
                simClock.start();

                // check if tool is touching an object
                JGenericObject objectContact = tool.getProxyPointForceModel().getContactPoint0().getObject();

                if (objectContact != null) {
                    // retrieve the root of the object mesh
                    JGenericObject obj = objectContact.getSuperParent();

                    // get position of cursor in global coordinates
                    JVector3d toolPos = tool.getDeviceGlobalPosition();

                    // get position of object in global coordinates
                    JVector3d objectPos = obj.getGlobalPosition();

                    // compute a vector from the center of mass of the object (point of rotation) to the tool
                    JVector3d vObjectCMToTool = JMaths.jSub(toolPos, objectPos);

                    // compute acceleration based on the interaction forces
                    // between the tool and the object
                    if (vObjectCMToTool.length() > 0.0) {
                        // get the last force applied to the cursor in global coordinates
                        // we negate the result to obtain the opposite force that is applied on the
                        // object
                        JVector3d toolForce = JMaths.jNegate(tool.getLastComputedGlobalForce());

                        // compute effective force to take into account the fact the object
                        // can only rotate around a its center mass and not translate
                        JVector3d effectiveForce = toolForce.operatorSub(JMaths.jProject(toolForce, vObjectCMToTool));

                        // compute the resulting torque
                        JVector3d torque = JMaths.jMul(vObjectCMToTool.length(), JMaths.jCross(JMaths.jNormalize(vObjectCMToTool), effectiveForce));

                        // update rotational acceleration
                        final double OBJECT_INERTIA = (1.0 / 0.4);
                        torque.mul(OBJECT_INERTIA);
                        rotAcc.add(torque);

                    }
                }

                // update rotational velocity
                rotAcc.mul(timeInterval);
                rotVel.add(rotAcc);

                // set a threshold on the rotational velocity term

                final double ROT_VEL_MAX = 10.0;
                double velMag = rotVel.length();

                if (velMag > ROT_VEL_MAX) {
                    rotVel.mul(ROT_VEL_MAX / velMag);
                }

                // add some damping too

                final double DAMPING_GAIN = 0.1;
                rotVel.mul(1.0 - DAMPING_GAIN * timeInterval);

                // if user switch is pressed, set velocity to zero
                if (tool.getUserSwitch(0) == true) {
                    rotVel.zero();
                }

                // compute the next rotation configuration of the object
                if (rotVel.length() > JConstants.CHAI_SMALL) {
                    //object.getRotate(JMaths.jNormalize(rotVel), timeInterval * rotVel.length());
                }
            }
        }
    }
}
