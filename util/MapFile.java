package util;

public class MapFile {
    String name;
    String extension;
    byte[] bytes;

    public MapFile(String name, String extension, byte[] bytes) {
        this.name = name;
        this.extension = extension;
        this.bytes = bytes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

}