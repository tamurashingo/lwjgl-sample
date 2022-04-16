package com.github.tamurashingo.lwjgl.sample;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class HelloWorld5 {

    private long window;

    private int programId;

    private int projectionMatrixUniform;
    private int worldMatrixUniform;

    private GameItem[] items;

    private static final String VERTEX_SHADER =
            "#version 330\n"
                    + "layout (location=0) in vec3 position;\n"
                    + "layout (location=1) in vec3 inColor;\n"

                    + "out vec3 exColor;\n"

                    + "uniform mat4 worldMatrix;\n"
                    + "uniform mat4 projectionMatrix;\n"

                    + "void main()\n"
                    + "{\n"
                    + "  gl_Position = projectionMatrix * worldMatrix * vec4(position, 1.0);\n"
                    + "  exColor = inColor;\n"
                    + "}\n"
            ;

    private static final String FRAGMENT_SHADER =
            "#version 330\n"
                    + "in vec3 exColor;\n"
                    + "out vec4 fragColor;\n"

                    + "void main()\n"
                    + "{\n"
                    + "  fragColor = vec4(exColor, 1.0);\n"
                    + "}\n"
            ;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        initShader();
        initObject();
        loop();
        cleanup();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(300, 300, "Hello World", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();
    }

    private void initObject() {
        // cube
        float[] positions = new float[] {
               -0.5f,  0.5f,  0.5f,
               -0.5f, -0.5f,  0.5f,
                0.5f, -0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
               -0.5f,  0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
               -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
        };
        float[] colors = new float[] {
                0.5f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.0f, 0.0f, 0.5f,
                0.0f, 0.5f, 0.5f,
                0.5f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.0f, 0.0f, 0.5f,
                0.0f, 0.5f, 0.5f,
        };
        int[] indices = new int[] {
                // front
                0, 1, 3, 3, 1, 2,
                // top
                4, 0, 3, 5, 4, 3,
                // right
                3, 2, 7, 5, 3, 7,
                // left
                6, 1, 0, 6, 0, 4,
                // bottom
                2, 1, 6, 2, 6, 7,
                // back
                7, 6, 4, 7, 4, 5,
        };

        Mesh mesh = new Mesh(positions, colors, indices);
        GameItem item = new GameItem(mesh);
        item.setPosition(0, 0, -2);
        items = new GameItem[] { item };
    }

    private void initShader() {
        programId = glCreateProgram();
        if (programId == NULL) {
            throw new RuntimeException("Could not create shader");
        }

        // vertex shader
        int vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
        if (vertexShaderId == NULL) {
            throw new RuntimeException("Could not create vertex shader");
        }
        glShaderSource(vertexShaderId, VERTEX_SHADER);
        glCompileShader(vertexShaderId);

        if (glGetShaderi(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error compiling Shader code: " + glGetShaderInfoLog(vertexShaderId, 1024));
        }

        glAttachShader(programId, vertexShaderId);

        // fragment shader
        int fragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);
        if (fragmentShaderId == NULL) {
            throw new RuntimeException("Could not create fragment shader");
        }
        glShaderSource(fragmentShaderId, FRAGMENT_SHADER);
        glCompileShader(fragmentShaderId);

        if (glGetShaderi(fragmentShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error compiling Shader code: " + glGetShaderInfoLog(fragmentShaderId, 1024));
        }

        glAttachShader(programId, fragmentShaderId);

        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        projectionMatrixUniform = glGetUniformLocation(programId, "projectionMatrix");
        if (projectionMatrixUniform < 0) {
            throw new RuntimeException("Could not find uniform: projectionMatrix");
        }

        worldMatrixUniform = glGetUniformLocation(programId, "worldMatrix");
        if (worldMatrixUniform < 0) {
            throw new RuntimeException("Could not find uniform: worldMatrix");
        }
    }

    private void loop() {
        glClearColor(1f, 1f, 1f, 0.0f);
        glEnable(GL_DEPTH_TEST);

        IntBuffer pWidth = MemoryUtil.memAllocInt(1);
        IntBuffer pHeight = MemoryUtil.memAllocInt(1);

        float rotation = 0;
        Transformation transformation = new Transformation();

        while (!glfwWindowShouldClose(window)) {
            float ratio;

            glfwGetFramebufferSize(window, pWidth, pHeight);
            ratio = pWidth.get() / (float) pHeight.get();

            pWidth.rewind();
            pHeight.rewind();

            glViewport(0, 0, pWidth.get(), pHeight.get());
            pWidth.rewind();
            pHeight.rewind();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            rotation = rotation + 1.5f;
            if (rotation > 360) {
                rotation = 0;
            }
            for (GameItem item: items) {
                item.setRotation(rotation, rotation, rotation);
            }


            // draw mesh
            glUseProgram(programId);

            // update projection matrix
            Matrix4f projectionMatrix = transformation.getProjectionMatrix((float)Math.toRadians(60.0f), pWidth.get(), pHeight.get(), 0.01f, 1000.f);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniformMatrix4fv(projectionMatrixUniform, false, projectionMatrix.get(stack.mallocFloat(16)));
            }

            // for Render each item
            for (GameItem item: items) {
                Matrix4f worldMatrix = transformation.getWorldMatrix(
                        item.getPosition(),
                        item.getRotaion(),
                        item.getScale()
                );

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    glUniformMatrix4fv(worldMatrixUniform, false, worldMatrix.get(stack.mallocFloat(16)));
                }
                glBindVertexArray(item.getMesh().getVaoId());
                glDrawElements(GL_TRIANGLES, item.getMesh().getVerTexCount(), GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);

            }
            glUseProgram(0);


            glfwSwapBuffers(window);
            glfwPollEvents();

            pWidth.flip();
            pHeight.flip();
        }

        MemoryUtil.memFree(pWidth);
        MemoryUtil.memFree(pHeight);
    }

    private void cleanup() {
        glUseProgram(0);
        glDeleteProgram(programId);

        for (GameItem item: items) {
            item.getMesh().cleanUp();
        }
    }

    public static void main(String...args) {
        new HelloWorld5().run();
    }
}
