package com.tibco.bpm.cdm.util;

import java.util.Arrays;
import java.util.Optional;

public enum TimestampOp {
	
	GREATER_THAN("gt"),
	LESS_THAN_EQUALS("le"),
	EQUALS("eq");
	
	private String opr;
	TimestampOp(String oprStr) {
		opr = oprStr;
	}
	public String getOpr() {
		return opr;
	}
	
    public static Optional<TimestampOp> get(String oprstr) {
        return Arrays.stream(TimestampOp.values())
            .filter(ts -> ts.opr.equals(oprstr))
            .findFirst();
    }	
}
