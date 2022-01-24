package com.java_polytech.pipeline_interfaces;

import java.io.OutputStream;

public interface IWriter extends IConsumer, IConfigurable, GetError, Runnable {
    RC setOutputStream(OutputStream output);
    void run();
}
