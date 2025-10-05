package com.a4sync.server.resource;

import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class RangeResource implements Resource {
    private final Resource resource;
    private final long start;
    private final long length;

    public RangeResource(Resource resource, long start, long length) {
        this.resource = resource;
        this.start = start;
        this.length = length;
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws IOException {
        InputStream is = resource.getInputStream();
        is.skip(start);
        return new LimitedInputStream(is, length);
    }

    // Implement other Resource methods by delegating to wrapped resource
    @Override public boolean exists() { return resource.exists(); }
    
    @Override 
    @NonNull
    public URL getURL() throws IOException { 
        return resource.getURL(); 
    }
    
    @Override 
    @NonNull
    public URI getURI() throws IOException { 
        return resource.getURI(); 
    }
    
    @Override 
    @NonNull
    public File getFile() throws IOException { 
        return resource.getFile(); 
    }
    
    @Override 
    public long contentLength() throws IOException { 
        return length; 
    }
    
    @Override 
    @NonNull
    public Resource createRelative(@NonNull String relativePath) throws IOException { 
        return resource.createRelative(relativePath); 
    }
    
    @Override 
    public String getFilename() { 
        return resource.getFilename(); 
    }
    
    @Override 
    @NonNull
    public String getDescription() { 
        return resource.getDescription(); 
    }
    
    @Override 
    public long lastModified() throws IOException { 
        return resource.lastModified(); 
    }

    private static class LimitedInputStream extends InputStream {
        private final InputStream wrapped;
        private long remaining;

        LimitedInputStream(InputStream wrapped, long limit) {
            this.wrapped = wrapped;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int result = wrapped.read();
            if (result != -1) remaining--;
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int result = wrapped.read(b, off, (int) Math.min(len, remaining));
            if (result != -1) remaining -= result;
            return result;
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }
    }
}
