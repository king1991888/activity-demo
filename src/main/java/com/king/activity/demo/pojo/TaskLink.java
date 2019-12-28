package com.king.activity.demo.pojo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author king
 * 2019/3/16
 */
@Setter
@Getter
public class TaskLink implements Serializable {


    private TaskLink previousTask;

    private TaskLink nextTask;


}
