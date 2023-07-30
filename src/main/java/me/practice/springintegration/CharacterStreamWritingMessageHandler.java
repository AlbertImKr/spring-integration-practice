package me.practice.springintegration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

public class CharacterStreamWritingMessageHandler extends AbstractMessageHandler {

    private final BufferedWriter writer;

    private volatile boolean shouldAppendNewLine = false;


    public CharacterStreamWritingMessageHandler(Writer writer) {
        this(writer, -1);
    }

    public CharacterStreamWritingMessageHandler(Writer writer, int bufferSize) {
        Assert.notNull(writer, "writer must not be null");
        if (writer instanceof BufferedWriter) {
            this.writer = (BufferedWriter) writer;
        }
        else if (bufferSize > 0) {
            this.writer = new BufferedWriter(writer, bufferSize);
        }
        else {
            this.writer = new BufferedWriter(writer);
        }
    }


    /**
     * Factory method that creates a target for stdout (System.out) with the
     * default charset encoding.
     *
     * @return A stdout handler with the default charset.
     */
    public static CharacterStreamWritingMessageHandler stdout() {
        return stdout(null);
    }

    /**
     * Factory method that creates a target for stdout (System.out) with the
     * specified charset encoding.
     *
     * @param charsetName The charset name.
     * @return A stdout handler.
     */
    public static CharacterStreamWritingMessageHandler stdout(String charsetName) {
        return createTargetForStream(System.out, charsetName);
    }

    /**
     * Factory method that creates a target for stderr (System.err) with the
     * default charset encoding.
     *
     * @return A stderr handler with the default charset.
     */
    public static CharacterStreamWritingMessageHandler stderr() {
        return stderr(null);
    }

    /**
     * Factory method that creates a target for stderr (System.err) with the
     * specified charset encoding.
     *
     * @param charsetName The charset name.
     * @return A stderr handler.
     */
    public static CharacterStreamWritingMessageHandler stderr(String charsetName) {
        return createTargetForStream(System.err, charsetName);
    }

    private static CharacterStreamWritingMessageHandler createTargetForStream(OutputStream stream, String charsetName) {
        if (charsetName == null) {
            return new CharacterStreamWritingMessageHandler(new OutputStreamWriter(stream));
        }
        try {
            return new CharacterStreamWritingMessageHandler(new OutputStreamWriter(stream, charsetName));
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("unsupported encoding: " + charsetName, e);
        }
    }


    public void setShouldAppendNewLine(boolean shouldAppendNewLine) {
        this.shouldAppendNewLine = shouldAppendNewLine;
    }

    /**
     * Fluent api for {@link #setShouldAppendNewLine(boolean)}.
     * @param append true to append a newline.
     * @return this.
     * @since 5.4
     */
    public CharacterStreamWritingMessageHandler appendNewLine(boolean append) {
        setShouldAppendNewLine(append);
        return this;
    }

    @Override
    public String getComponentType() {
        return "stream:outbound-channel-adapter(character)";
    }

    @Override
    protected void handleMessageInternal(Message<?> message) {
        Object payload = message.getPayload();
        try {
            if (payload instanceof String) {
                this.writer.write((String) payload);
            }
            else if (payload instanceof char[]) {
                this.writer.write((char[]) payload);
            }
            else if (payload instanceof byte[]) {
                this.writer.write(new String((byte[]) payload));
            }
            else if (payload instanceof Exception) {
                PrintWriter printWriter = new PrintWriter(this.writer, true);
                ((Exception) payload).printStackTrace(printWriter);
            }
            else {
                this.writer.write(payload.toString());
            }
            if (this.shouldAppendNewLine) {
                this.writer.newLine();
            }
            this.writer.flush();
        }
        catch (IOException e) {
            throw new MessagingException("IO failure occurred in target", e);
        }
    }

}
