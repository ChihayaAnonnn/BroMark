package com.jay.test.v2;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import org.junit.Test;

public class BuildBaselineV2Test {
  @Test
  public void runtimeAndApplicationBytecodeUseJava25() throws Exception {
    assertEquals("25", System.getProperty("java.specification.version"));
    try (DataInputStream in =
        new DataInputStream(
            com.jay.Application.class.getResourceAsStream("/com/jay/Application.class"))) {
      assertEquals(0xCAFEBABE, in.readInt());
      in.readUnsignedShort();
      assertEquals(69, in.readUnsignedShort());
    }
  }
}
