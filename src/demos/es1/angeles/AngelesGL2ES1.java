/* San Angeles Observation OpenGL ES version example
 * Copyright 2004-2005 Jetro Lauha
 * All rights reserved.
 * Web: http://iki.fi/jetro/
 *
 * This source is free software; you can redistribute it and/or
 * modify it under the terms of EITHER:
 *   (1) The GNU Lesser General Public License as published by the Free
 *       Software Foundation; either version 2.1 of the License, or (at
 *       your option) any later version. The text of the GNU Lesser
 *       General Public License is included with this source in the
 *       file LICENSE-LGPL.txt.
 *   (2) The BSD-style license that is included with this source in
 *       the file LICENSE-BSD.txt.
 *
 * This source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files
 * LICENSE-LGPL.txt and LICENSE-BSD.txt for more details.
 *
 * $Id$
 * $Revision$
 */

package demos.es1.angeles;

import javax.media.opengl.*;
import javax.media.opengl.util.*;
import javax.media.opengl.glu.*;
import java.nio.*;

public class AngelesGL2ES1 implements GLEventListener {

    public AngelesGL2ES1() {
        quadVertices = BufferUtil.newFloatBuffer(12);
        quadVertices.put(new float[]{
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f, -1.0f,
             1.0f,  1.0f,
            -1.0f,  1.0f
        });
        quadVertices.flip();

        light0Position=BufferUtil.newFloatBuffer(4);
        light0Diffuse=BufferUtil.newFloatBuffer(4);
        light1Position=BufferUtil.newFloatBuffer(4);
        light1Diffuse=BufferUtil.newFloatBuffer(4);
        light2Position=BufferUtil.newFloatBuffer(4);
        light2Diffuse=BufferUtil.newFloatBuffer(4);
        materialSpecular=BufferUtil.newFloatBuffer(4);

        light0Position.put(new float[] { FixedPoint.toFloat(-0x40000), 1.0f, 1.0f, 0.0f });
        light0Diffuse.put(new float[] { 1.0f, FixedPoint.toFloat(0x6666), 0.0f, 1.0f });
        light1Position.put(new float[] { 1.0f, FixedPoint.toFloat(-0x20000), -1.0f, 0.0f });
        light1Diffuse.put(new float[] { FixedPoint.toFloat(0x11eb), FixedPoint.toFloat(0x23d7), FixedPoint.toFloat(0x5999), 1.0f });
        light2Position.put(new float[] { -1.0f, 0.0f, FixedPoint.toFloat(-0x40000), 0.0f });
        light2Diffuse.put(new float[] { FixedPoint.toFloat(0x11eb), FixedPoint.toFloat(0x2b85), FixedPoint.toFloat(0x23d7), 1.0f });
        materialSpecular.put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });

        light0Position.flip();
        light0Diffuse.flip();
        light1Position.flip();
        light1Diffuse.flip();
        light2Position.flip();
        light2Diffuse.flip();
        materialSpecular.flip();

        seedRandom(15);

        width=0;
        height=0;
        x=0;
        y=0;
    }

    public void init(GLAutoDrawable drawable) {
        // FIXME: gl.setSwapInterval(1);

        this.gl = drawable.getGL().getGL2ES1();
        this.glu = GLU.createGLU();
        if(gl.isGLES2()) {
            gl.getGLES2().enableFixedFunctionEmulationMode(GLES2.FIXED_EMULATION_VERTEXCOLORTEXTURE);
            System.err.println("AngelesGL2ES1 Fixed emu: FIXED_EMULATION_VERTEXCOLORTEXTURE");
        }
        gl.glEnable(GL2ES1.GL_NORMALIZE);
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_CULL_FACE);
        gl.glShadeModel(gl.GL_FLAT);

        gl.glEnable(gl.GL_LIGHTING);
        gl.glEnable(gl.GL_LIGHT0);
        gl.glEnable(gl.GL_LIGHT1);
        gl.glEnable(gl.GL_LIGHT2);

        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);

        for (int a = 0; a < SuperShape.COUNT; ++a)
        {
            sSuperShapeObjects[a] = createSuperShape(SuperShape.sParams[a]);
        }
        sGroundPlane = createGroundPlane();

        gAppAlive = 1;

        sStartTick = System.currentTimeMillis();
        frames=0;
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = width;
        this.height=height;
        this.x = x;
        this.y = y;

        this.gl = drawable.getGL().getGL2ES1();

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glClearColor(0.1f, 0.2f, 0.3f, 1.0f);

        gl.glCullFace(GL.GL_FRONT);

        gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_FASTEST);

        //gl.glShadeModel(GL.GL_SMOOTH);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glDisable(gl.GL_DITHER);

        //gl.glMatrixMode(gl.GL_PROJECTION);
        //gl.glLoadIdentity();
        //glu.gluPerspective(45.0f, (float)width / (float)height, 0.5f, 150.0f);

        System.out.println("reshape ..");
    }

    public void display(GLAutoDrawable drawable) {
        long tick = System.currentTimeMillis();

        if (gAppAlive==0)
            return;

        this.gl = drawable.getGL().getGL2ES1();

        // Actual tick value is "blurred" a little bit.
        sTick = (sTick + tick - sStartTick) >> 1;

        // Terminate application after running through the demonstration once.
        if (sTick >= RUN_LENGTH)
        {
            gAppAlive = 0;
            return;
        }

        gl.glClear(gl.GL_DEPTH_BUFFER_BIT | gl.GL_COLOR_BUFFER_BIT);

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, (float)width / (float)height, 0.5f, 150.0f);

        // Update the camera position and set the lookat.
        camTrack();

        // Configure environment.
        configureLightAndMaterial();

        // Draw the reflection by drawing models with negated Z-axis.
        gl.glPushMatrix();
        drawModels(-1);
        gl.glPopMatrix();

        // Blend the ground plane to the window.
        drawGroundPlane();

        // Draw all the models normally.
        drawModels(1);

        // Draw fade quad over whole window (when changing cameras).
        drawFadeQuad();

        frames++;
        tick = System.currentTimeMillis();
        long dT = tick - sStartTick;
        //        System.out.println(frames+"f, "+dT+"ms "+ (frames*1000)/dT +"fps");
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

 private GL2ES1 gl;
 private GLU glu;

 // Total run length is 20 * camera track base unit length (see cams.h).
 private int RUN_LENGTH  = (20 * CamTrack.CAMTRACK_LEN) ;
 private int RANDOM_UINT_MAX = 65535 ;

 private long sRandomSeed = 0;

void seedRandom(long seed)
{
    sRandomSeed = seed;
}

int randomUInt()
{
    sRandomSeed = sRandomSeed * 0x343fd + 0x269ec3;
    return Math.abs((int) (sRandomSeed >> 16));
}


// Definition of one GL object in this demo.
public class GLObject {
    /* Vertex array and color array are enabled for all objects, so their
     * pointers must always be valid and non-null. Normal array is not
     * used by the ground plane, so when its pointer is null then normal
     * array usage is disabled.
     *
     * Vertex array is supposed to use gl.GL_FLOAT datatype and stride 0
     * (i.e. tightly packed array). Color array is supposed to have 4
     * components per color with gl.GL_UNSIGNED_BYTE datatype and stride 0.
     * Normal array is supposed to use gl.GL_FLOAT datatype and stride 0.
     */
    FloatBuffer vertexArray;
    ByteBuffer colorArray;
    FloatBuffer normalArray;
    int vertexComponents;
    int count;
    int vbo[];
    
    public GLObject(int vertices, int vertexComponents,
                    boolean useNormalArray) {
        this.count = vertices;
        this.vertexComponents = vertexComponents;
        this.vertexArray = BufferUtil.newFloatBuffer( vertices * vertexComponents );
        this.colorArray =  BufferUtil.newByteBuffer (vertices * 4 );
        if (useNormalArray)
        {
            this.normalArray = BufferUtil.newFloatBuffer (vertices * 3 );
        } else {
            this.normalArray = null;
        }
    }

    void seal()
    {
        flip();
        vbo = new int[3];
        gl.glGenBuffers(3, vbo, 0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        if(null==vertexArray) {
            throw new RuntimeException("vertexArray is null");
        }
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexArray.capacity() * BufferUtil.SIZEOF_FLOAT, vertexArray, GL.GL_STATIC_DRAW);
        gl.glVertexPointer(vertexComponents, gl.GL_FLOAT, 0, 0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, colorArray.capacity() * BufferUtil.SIZEOF_BYTE, colorArray, GL.GL_STATIC_DRAW);
        gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, 0);

        if (null!=normalArray)
        {
            gl.glEnableClientState(gl.GL_NORMAL_ARRAY);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[2]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, normalArray.capacity() * BufferUtil.SIZEOF_FLOAT, normalArray, GL.GL_STATIC_DRAW);
            gl.glNormalPointer(gl.GL_FLOAT, 0, 0);
        } else {
            gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
        }
    }

    void draw()
    {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexPointer(vertexComponents, gl.GL_FLOAT, 0, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
        gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, 0);

        if (null!=normalArray)
        {
            gl.glEnableClientState(gl.GL_NORMAL_ARRAY);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[2]);
            gl.glNormalPointer(gl.GL_FLOAT, 0, 0);
        }
        else
            gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
        gl.glDrawArrays(gl.GL_TRIANGLES, 0, count);
    }

    void rewind() {
        vertexArray.rewind();
        colorArray.rewind();
        if (normalArray != null) {
            normalArray.rewind();
        }
    }
    void flip() {
        vertexArray.flip();
        colorArray.flip();
        if (normalArray != null) {
            normalArray.flip();
        }
    }
}

long sStartTick = 0;
long sTick = 0;

int sCurrentCamTrack = 0;
long sCurrentCamTrackStartTick = 0;
long sNextCamTrackStartTick = 0x7fffffff;

GLObject sSuperShapeObjects[] = new GLObject[SuperShape.COUNT];
GLObject sGroundPlane;


public class VECTOR3 {
    float x, y, z;

    public VECTOR3() {
        x=0f; y=0f; z=0f;
    }
    public VECTOR3(float x, float y, float z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }
}



static void vector3Sub(VECTOR3 dest, VECTOR3 v1, VECTOR3 v2)
{
    dest.x = v1.x - v2.x;
    dest.y = v1.y - v2.y;
    dest.z = v1.z - v2.z;
}


static void superShapeMap(VECTOR3 point, float r1, float r2, float t, float p)
{
    // sphere-mapping of supershape parameters
    point.x = (float)(Math.cos(t) * Math.cos(p) / r1 / r2);
    point.y = (float)(Math.sin(t) * Math.cos(p) / r1 / r2);
    point.z = (float)(Math.sin(p) / r2);
}


float ssFunc(final float t, final float p[])
{
    return ssFunc(t, p, 0);
}

float ssFunc(final float t, final float p[], int pOff)
{
    return (float)(Math.pow(Math.pow(Math.abs(Math.cos(p[0+pOff] * t / 4)) / p[1+pOff], p[4+pOff]) +
                            Math.pow(Math.abs(Math.sin(p[0+pOff] * t / 4)) / p[2+pOff], p[5+pOff]), 1 / p[3+pOff]));
}


// Creates and returns a supershape object.
// Based on Paul Bourke's POV-Ray implementation.
// http://astronomy.swin.edu.au/~pbourke/povray/supershape/
GLObject createSuperShape(final float params[])
{
    final int resol1 = (int)params[SuperShape.PARAMS - 3];
    final int resol2 = (int)params[SuperShape.PARAMS - 2];
    // latitude 0 to pi/2 for no mirrored bottom
    // (latitudeBegin==0 for -pi/2 to pi/2 originally)
    final int latitudeBegin = resol2 / 4;
    final int latitudeEnd = resol2 / 2;    // non-inclusive
    final int longitudeCount = resol1;
    final int latitudeCount = latitudeEnd - latitudeBegin;
    final int triangleCount = longitudeCount * latitudeCount * 2;
    final int vertices = triangleCount * 3;
    GLObject result;
    float baseColor[] = new float[3];
    int a, longitude, latitude;
    int currentVertex, currentQuad;

    result = new GLObject(vertices, 3, true);
    if (result == null)
        return null;

    for (a = 0; a < 3; ++a)
        baseColor[a] = ((randomUInt() % 155) + 100) / 255.f;

    currentQuad = 0;
    currentVertex = 0;

    // longitude -pi to pi
    for (longitude = 0; longitude < longitudeCount; ++longitude)
    {

        // latitude 0 to pi/2
        for (latitude = latitudeBegin; latitude < latitudeEnd; ++latitude)
        {
            float t1 = (float) ( -Math.PI + longitude * 2 * Math.PI / resol1 );
            float t2 = (float) ( -Math.PI + (longitude + 1) * 2 * Math.PI / resol1 );
            float p1 = (float) ( -Math.PI / 2 + latitude * 2 * Math.PI / resol2 );
            float p2 = (float) ( -Math.PI / 2 + (latitude + 1) * 2 * Math.PI / resol2 );
            float r0, r1, r2, r3;

            r0 = ssFunc(t1, params);
            r1 = ssFunc(p1, params, 6);
            r2 = ssFunc(t2, params);
            r3 = ssFunc(p2, params, 6);

            if (r0 != 0 && r1 != 0 && r2 != 0 && r3 != 0)
            {
                VECTOR3 pa=new VECTOR3(), pb=new VECTOR3(), pc=new VECTOR3(), pd=new VECTOR3();
                VECTOR3 v1=new VECTOR3(), v2=new VECTOR3(), n=new VECTOR3();
                float ca;
                int i;
                //float lenSq, invLenSq;

                superShapeMap(pa, r0, r1, t1, p1);
                superShapeMap(pb, r2, r1, t2, p1);
                superShapeMap(pc, r2, r3, t2, p2);
                superShapeMap(pd, r0, r3, t1, p2);

                // kludge to set lower edge of the object to fixed level
                if (latitude == latitudeBegin + 1)
                    pa.z = pb.z = 0;

                vector3Sub(v1, pb, pa);
                vector3Sub(v2, pd, pa);

                // Calculate normal with cross product.
                /*   i    j    k      i    j
                 * v1.x v1.y v1.z | v1.x v1.y
                 * v2.x v2.y v2.z | v2.x v2.y
                 */

                n.x = v1.y * v2.z - v1.z * v2.y;
                n.y = v1.z * v2.x - v1.x * v2.z;
                n.z = v1.x * v2.y - v1.y * v2.x;

                /* Pre-normalization of the normals is disabled here because
                 * they will be normalized anyway later due to automatic
                 * normalization (gl.GL_NORMALIZE). It is enabled because the
                 * objects are scaled with glScale.
                 */
                /*
                lenSq = n.x * n.x + n.y * n.y + n.z * n.z;
                invLenSq = (float)(1 / sqrt(lenSq));
                n.x *= invLenSq;
                n.y *= invLenSq;
                n.z *= invLenSq;
                */

                ca = pa.z + 0.5f;

                for (i = currentVertex * 3;
                     i < (currentVertex + 6) * 3;
                     i += 3)
                {
                    result.normalArray.put(i    , (n.x));
                    result.normalArray.put(i + 1, (n.y));
                    result.normalArray.put(i + 2, (n.z));
                }
                for (i = currentVertex * 4;
                     i < (currentVertex + 6) * 4;
                     i += 4)
                {
                    int j, color[] = new int[3];
                    for (j = 0; j < 3; ++j)
                    {
                        color[j] = (int)(ca * baseColor[j] * 255);
                        if (color[j] > 255) color[j] = 255;
                    }
                    result.colorArray.put(i    , (byte)color[0]);
                    result.colorArray.put(i + 1, (byte)color[1]);
                    result.colorArray.put(i + 2, (byte)color[2]);
                    result.colorArray.put(i + 3, (byte)0);
                }
                result.vertexArray.put(currentVertex * 3, (pa.x));
                result.vertexArray.put(currentVertex * 3 + 1, (pa.y));
                result.vertexArray.put(currentVertex * 3 + 2, (pa.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, (pb.x));
                result.vertexArray.put(currentVertex * 3 + 1, (pb.y));
                result.vertexArray.put(currentVertex * 3 + 2, (pb.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, (pd.x));
                result.vertexArray.put(currentVertex * 3 + 1, (pd.y));
                result.vertexArray.put(currentVertex * 3 + 2, (pd.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, (pb.x));
                result.vertexArray.put(currentVertex * 3 + 1, (pb.y));
                result.vertexArray.put(currentVertex * 3 + 2, (pb.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, (pc.x));
                result.vertexArray.put(currentVertex * 3 + 1, (pc.y));
                result.vertexArray.put(currentVertex * 3 + 2, (pc.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, (pd.x));
                result.vertexArray.put(currentVertex * 3 + 1, (pd.y));
                result.vertexArray.put(currentVertex * 3 + 2, (pd.z));
                ++currentVertex;
            } // r0 && r1 && r2 && r3
            ++currentQuad;
        } // latitude
    } // longitude

    // Set number of vertices in object to the actual amount created.
    result.count = currentVertex;
    result.seal();
    return result;
}


GLObject createGroundPlane()
{
    final  int scale = 4;
    final  int yBegin = -15, yEnd = 15;    // ends are non-inclusive
    final  int xBegin = -15, xEnd = 15;
    final  int triangleCount = (yEnd - yBegin) * (xEnd - xBegin) * 2;
    final  int vertices = triangleCount * 3;
    GLObject result;
    int x, y;
    int currentVertex, currentQuad;

    result = new GLObject(vertices, 2, false);
    if (result == null)
        return null;

    currentQuad = 0;
    currentVertex = 0;

    for (y = yBegin; y < yEnd; ++y)
    {
        for (x = xBegin; x < xEnd; ++x)
        {
            byte color;
            int i, a;
            color = (byte)((randomUInt() & 0x5f) + 81);  // 101 1111
            for (i = currentVertex * 4; i < (currentVertex + 6) * 4; i += 4)
            {
                result.colorArray.put(i, color);
                result.colorArray.put(i + 1, color);
                result.colorArray.put(i + 2, color);
                result.colorArray.put(i + 3, (byte)0);
            }

            // Axis bits for quad triangles:
            // x: 011100 (0x1c), y: 110001 (0x31)  (clockwise)
            // x: 001110 (0x0e), y: 100011 (0x23)  (counter-clockwise)
            for (a = 0; a < 6; ++a)
            {
                final int xm = x + ((0x1c >> a) & 1);
                final int ym = y + ((0x31 >> a) & 1);
                final float m = (float)(Math.cos(xm * 2) * Math.sin(ym * 4) * 0.75f);
                result.vertexArray.put(currentVertex * 2, (xm * scale + m));
                result.vertexArray.put(currentVertex * 2 + 1, (ym * scale + m));
                ++currentVertex;
            }
            ++currentQuad;
        }
    }
    result.seal();
    return result;
}


void drawGroundPlane()
{
    gl.glDisable(gl.GL_CULL_FACE);
    gl.glDisable(gl.GL_DEPTH_TEST);
    gl.glEnable(gl.GL_BLEND);
    gl.glBlendFunc(gl.GL_ZERO, gl.GL_SRC_COLOR);
    gl.glDisable(gl.GL_LIGHTING);

    sGroundPlane.draw();

    gl.glEnable(gl.GL_LIGHTING);
    gl.glDisable(gl.GL_BLEND);
    gl.glEnable(gl.GL_DEPTH_TEST);
}

void drawFadeQuad()
{
    final int beginFade = (int) (sTick - sCurrentCamTrackStartTick);
    final int endFade = (int) (sNextCamTrackStartTick - sTick);
    final int minFade = beginFade < endFade ? beginFade : endFade;

    if (minFade < 1024)
    {
        final float fadeColor = FixedPoint.toFloat(minFade << 7);
        gl.glColor4f(fadeColor, fadeColor, fadeColor, 0f);

        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glEnable(gl.GL_BLEND);
        gl.glBlendFunc(gl.GL_ZERO, gl.GL_SRC_COLOR);
        gl.glDisable(gl.GL_LIGHTING);

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(gl.GL_COLOR_ARRAY);
        gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
        gl.glVertexPointer(2, gl.GL_FLOAT, 0, quadVertices);
        gl.glDrawArrays(gl.GL_TRIANGLES, 0, 6);

        gl.glEnableClientState(gl.GL_COLOR_ARRAY);

        gl.glMatrixMode(gl.GL_MODELVIEW);

        gl.glEnable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_BLEND);
        gl.glEnable(gl.GL_DEPTH_TEST);
    }
}

FloatBuffer quadVertices;
FloatBuffer light0Position;
FloatBuffer light0Diffuse;
FloatBuffer light1Position;
FloatBuffer light1Diffuse;
FloatBuffer light2Position;
FloatBuffer light2Diffuse;
FloatBuffer materialSpecular;

void configureLightAndMaterial()
{
    gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, light0Position);
    gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, light0Diffuse);
    gl.glLightfv(gl.GL_LIGHT1, gl.GL_POSITION, light1Position);
    gl.glLightfv(gl.GL_LIGHT1, gl.GL_DIFFUSE, light1Diffuse);
    gl.glLightfv(gl.GL_LIGHT2, gl.GL_POSITION, light2Position);
    gl.glLightfv(gl.GL_LIGHT2, gl.GL_DIFFUSE, light2Diffuse);
    gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_SPECULAR, materialSpecular);

    gl.glMaterialf(gl.GL_FRONT_AND_BACK, gl.GL_SHININESS, 60.0f);
    gl.glEnable(gl.GL_COLOR_MATERIAL);
}


void drawModels(float zScale)
{
    final int translationScale = 9;
    int x, y;

    seedRandom(9);

    gl.glScalef(1.0f, 1.0f, zScale);

    for (y = -5; y <= 5; ++y)
    {
        for (x = -5; x <= 5; ++x)
        {
            int curShape = randomUInt() % SuperShape.COUNT;
            float buildingScale = SuperShape.sParams[curShape][SuperShape.PARAMS - 1];

            gl.glPushMatrix();
            gl.glTranslatef((float)(x * translationScale),
                            (float)(y * translationScale),
                            0f);
            gl.glRotatef((float)(randomUInt() % 360), 0f, 0f, 1f);
            gl.glScalef(buildingScale, buildingScale, buildingScale);

            sSuperShapeObjects[curShape].draw();
            gl.glPopMatrix();
        }
    }

    for (x = -2; x <= 2; ++x)
    {
        final int shipScale100 = translationScale * 500;
        final int offs100 = x * shipScale100 + (int)(sTick % shipScale100);
        float offs = offs100 * 0.01f;
        gl.glPushMatrix();
        gl.glTranslatef(offs, -4.0f, 2.0f);
        sSuperShapeObjects[SuperShape.COUNT - 1].draw();
        gl.glPopMatrix();
        gl.glPushMatrix();
        gl.glTranslatef(-4.0f, offs, 4.0f);
        gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
        sSuperShapeObjects[SuperShape.COUNT - 1].draw();
        gl.glPopMatrix();
    }
}


void camTrack()
{
    float lerp[]= new float[5];
    float eX, eY, eZ, cX, cY, cZ;
    float trackPos;
    CamTrack cam;
    long currentCamTick;
    int a;

    if (sNextCamTrackStartTick <= sTick)
    {
        ++sCurrentCamTrack;
        sCurrentCamTrackStartTick = sNextCamTrackStartTick;
    }
    sNextCamTrackStartTick = sCurrentCamTrackStartTick +
                             CamTrack.sCamTracks[sCurrentCamTrack].len * CamTrack.CAMTRACK_LEN;

    cam = CamTrack.sCamTracks[sCurrentCamTrack];
    currentCamTick = sTick - sCurrentCamTrackStartTick;
    trackPos = (float)currentCamTick / (CamTrack.CAMTRACK_LEN * cam.len);

    for (a = 0; a < 5; ++a)
        lerp[a] = (cam.src[a] + cam.dest[a] * trackPos) * 0.01f;

    if (cam.dist>0)
    {
        float dist = cam.dist * 0.1f;
        cX = lerp[0];
        cY = lerp[1];
        cZ = lerp[2];
        eX = cX - (float)Math.cos(lerp[3]) * dist;
        eY = cY - (float)Math.sin(lerp[3]) * dist;
        eZ = cZ - lerp[4];
    }
    else
    {
        eX = lerp[0];
        eY = lerp[1];
        eZ = lerp[2];
        cX = eX + (float)Math.cos(lerp[3]);
        cY = eY + (float)Math.sin(lerp[3]);
        cZ = eZ + lerp[4];
    }
    glu.gluLookAt(eX, eY, eZ, cX, cY, cZ, 0, 0, 1);
}

private int gAppAlive = 0;
private int width, height, x, y, frames;
}
