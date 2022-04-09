package com.github.tamurashingo.lwjgl.sample;

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

public class HelloWorld3 {

    private long window;

    private int vaoId;
    private int vboId;
    private int programId;

    private static final String VERTEX_SHADER =
              "#version 330\n"
            + "layout (location=0) in vec3 position;\n"
            + "void main()\n"
            + "{\n"
            + "  gl_Position = vec4(position, 1.0);\n"
            + "}\n"
            ;

    private static final String FRAGMENT_SHADER =
              "#version 330\n"
            + "out vec4 fragColor;\n"
            + "void main()\n"
            + "{\n"
            + "  fragColor = vec4(1.0, 0.0, 0.0, 1.0);\n"
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
        float[] vertex = new float[]{
                0.0f,  0.5f, 1.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, -1.0f,
        };
        FloatBuffer verticesbuffer = MemoryUtil.memAllocFloat(vertex.length);
        try {
            verticesbuffer.put(vertex);
            verticesbuffer.flip();

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, verticesbuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } finally {
            MemoryUtil.memFree(verticesbuffer);
        }
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
    }

    private void loop() {
        glClearColor(1f, 1f, 1f, 0.0f);

        IntBuffer pWidth = MemoryUtil.memAllocInt(1);
        IntBuffer pHeight = MemoryUtil.memAllocInt(1);

        while (!glfwWindowShouldClose(window)) {
            float ratio;

            glfwGetFramebufferSize(window, pWidth, pHeight);
            ratio = pWidth.get() / (float) pHeight.get();

            pWidth.rewind();
            pHeight.rewind();

            glViewport(0, 0, pWidth.get(), pHeight.get());

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


            glUseProgram(programId);
            glBindVertexArray(vaoId);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);
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

        glDisableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboId);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    public static void main(String...args) {
        new HelloWorld3().run();
    }
}
