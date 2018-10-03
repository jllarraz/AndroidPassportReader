package example.jllarraz.com.passportreader.ui.validators;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.widget.EditText;

import com.mobsandgeeks.saripaar.QuickRule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import example.jllarraz.com.passportreader.R;


/**
 * Created by Surface on 15/08/2017.
 */

public class DocumentNumberRule extends QuickRule<AppCompatEditText> {

    private static String REGEX ="[A-Z0-9<]{9}$";

    @Override
    public boolean isValid(AppCompatEditText editText) {
        String text = editText.getText().toString().trim();
        Pattern patternDate = Pattern.compile(REGEX);
        Matcher matcherDate = patternDate.matcher(text);
        return matcherDate.find();
    }

    @Override
    public String getMessage(Context context) {
        return context.getString(R.string.error_validation_document_number);
    }
}
