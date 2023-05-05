package Xuoos.GenshinImpact.Proxy;

import com.github.megatronking.stringfog.IStringFog;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;

public class StringFogImpl implements IStringFog {

    private final Method descriptor = StringFogImpl.class.getDeclaredMethods()[0];

    public static String _decrypt(byte[] data, byte[] key) {
        return new String(xor(data, key), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encrypt(String data, byte[] key) {
        return xor(data.getBytes(StandardCharsets.UTF_8), key);
    }

    @Override
    public String decrypt(byte[] data, byte[] key) {
      try {
          return (String) descriptor.invoke(null, data, key);
      } catch (ReflectiveOperationException e) {
          return null;
      }
    }

    @Override
    public boolean shouldFog(String data) {
        return true;
    }

    private static byte[] xor(byte[] data, byte[] key) {
        int len = data.length;
        int lenKey = key.length;
        int i = 0;
        int j = 0;
        while (i < len) {
            if (j >= lenKey) {
                j = 0;
            }
            data[i] = (byte) (data[i] ^ key[j]);
            i++;
            j++;
        }
        return data;
    }
}
