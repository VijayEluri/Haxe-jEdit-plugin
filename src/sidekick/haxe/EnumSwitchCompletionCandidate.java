package sidekick.haxe;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import superabbrevs.SuperAbbrevs;

import completion.util.CompletionUtil;
import completion.util.CtagsCompletionCandidate;

import ctagsinterface.main.Tag;

public class EnumSwitchCompletionCandidate extends CtagsCompletionCandidate
{
    //The tag has to be a haxe enum
    public EnumSwitchCompletionCandidate (Tag tag)
    {
        super(tag);
    }

    @Override
    //We assume that the caret is at the star:  switch*
    public void complete (View view)
    {
        TextArea textArea = view.getTextArea();
        String prefix = CompletionUtil.getCompletionPrefix(view);
        int caret = textArea.getCaretPosition();
        JEditBuffer buffer = textArea.getBuffer();

        if (prefix.length() > 0) {
            buffer.remove(caret - prefix.length(), prefix.length());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("switch ($1) {\n");
        int count = 2;
        for (String e : getEnumConstructors(view, tag)) {
            sb.append("\tcase " + e + ": $" + (count++) + "\n");
        }
        sb.append("}$end\n");

        SuperAbbrevs.expandAbbrev(view, sb.toString(), null);
    }

    @Override
    public boolean isValid (View view)
    {
        String prefix = CompletionUtil.getCompletionPrefix(view);
        prefix = prefix.replace("switch", "");
        if (prefix == null || prefix.length() == 0) {
            return true;
        }
        return tag.getName().toLowerCase().startsWith(prefix.toLowerCase());
    }

    public static List<String> getEnumConstructors (View view, Tag tag)
    {
        List<String> enums = new ArrayList<String>();
        try{
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(tag.getFile());
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            boolean atEnum = false;
            //Read File Line By Line
            while ((line = br.readLine()) != null)   {
                if (line.matches(".*}.*")) {
                    break;
                }
                if (atEnum) {
                    //Ignore empty lines and comments
                    if (!line.trim().matches("^[ \t]*(/\\*|//|#)+.*") && line.trim().length() > 0) {
                        //Remove the trailing semicolon
                        if (line.trim().replace(";", "").length() > 0) {
                            enums.add(line.trim().replace(";", ""));
                        }
                    }
                } else if (line.matches("(^|[.* \t]+)enum[ \t{<]+.*")) {
                    atEnum = true;
                }
            }
            //Close the input stream
            in.close();
        } catch (Exception e){//Catch exception if any
          System.err.println("Error: " + e.getMessage());
        }
        return enums;
    }
}
