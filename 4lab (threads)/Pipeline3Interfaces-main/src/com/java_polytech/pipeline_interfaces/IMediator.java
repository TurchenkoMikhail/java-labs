package com.java_polytech.pipeline_interfaces;

import javafx.util.Pair;

public interface IMediator {
    // if result is NULL, then it is end and prepare for destroy
    Pair<Integer, Object> getData();
}
