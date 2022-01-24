package com.java_polytech.pipeline_interfaces;

import java.io.InputStream;

public interface IReader extends IProvider, IConfigurable,GetError,Runnable {
    RC setInputStream(InputStream input);

    void run();

}
