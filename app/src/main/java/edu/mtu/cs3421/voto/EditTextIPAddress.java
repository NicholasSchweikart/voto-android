package edu.mtu.cs3421.voto;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;

/**
 * Borrowed from Android UPD-Sender @
 * https://github.com/hastarin/android-udpsender
 *
 * Simple edit text taylored to only allow valid IP addresses. It also will auto append a '.' as
 * you type in the numbers. 
 */
public class EditTextIPAddress extends android.support.v7.widget.AppCompatEditText {

    public EditTextIPAddress(Context context) {
        super(context);
        initialize();
    }

    public EditTextIPAddress(Context context, AttributeSet attrs) {

        super(context, attrs);
        initialize();
    }

    public EditTextIPAddress(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);
        initialize();
    }

    @Override
    public void setInputType(int type)
    {
        super.setInputType(type);
        HandleInputType(type);
    }

    private void HandleInputType(int type)
    {
        switch (type) {
            case InputType.TYPE_CLASS_PHONE:
                setFilters(new InputFilter[] { new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               android.text.Spanned destination, int dstart, int dend) {
                        if (end > start) {
                            String destinationString= destination.toString();
                            String resultingText = destinationString.substring(0, dstart)
                                    + source.subSequence(start, end)
                                    + destinationString.substring(dend);
                            if (!resultingText
                                    .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                                return "";
                            } else {
                                String[] splits = resultingText.split("\\.");
                                for (int i = 0; i < splits.length; i++) {
                                    if (Integer.valueOf(splits[i]) > 255) {
                                        return "";
                                    }
                                }
                            }
                        }
                        return null;
                    }
                } });
                break;
            default:
                setFilters(new InputFilter[]{});
                break;
        }
    }

    private void initialize() {

        HandleInputType(getInputType());

        addTextChangedListener(new TextWatcher() {
            boolean deleting = false;
            int lastCount = 0;

            @Override
            public void afterTextChanged(Editable s) {
                boolean check = getInputType() == InputType.TYPE_CLASS_PHONE;
                if (check && !deleting) {
                    String working = s.toString();
                    String[] split = working.split("\\.");
                    String string = split[split.length - 1];
                    if (string.length() == 3
                            || string.equalsIgnoreCase("0")
                            || (string.length() == 2 && Integer.parseInt(string) > 25))
                    {
                        s.append('.');
                        return;
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (lastCount < count) {
                    deleting = false;
                } else {
                    deleting = true;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // Nothing happens here
            }
        });
    }
}