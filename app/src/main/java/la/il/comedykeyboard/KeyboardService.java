package la.il.comedykeyboard;

import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.media.AudioManager;
import android.os.Build.VERSION;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author Amir Lazarovich
 */
public class KeyboardService extends InputMethodService implements OnKeyboardActionListener {
    private static final int KEYCODE_SPACE = 32;
    private static final int KEYCODE_OK = 10;

    private Drawable mIcShift;
    private Drawable mIcShiftLocked;
    private KeyboardView mKeyboardView;
    private Keyboard mKeyboard;
    private CandidateView mCandidateView;
    private boolean mCaps = false;
    private static final List<String> SUGGESTIONS = Arrays.asList(
            "Bars sound like I'm under oath nigga. I comedy central roast niggas and turn 'em to ghost niggas.",
            "People know me. I'm not going to produce any cartwheels out there. I'm not going to belong on comedy central. I'll always be a tennis player, not a celebrity.",
            "Honestly we never lied to people about who we were. Usually the wackier interviews came to pass because the interview subjects, aware that we were comedy central, just wanted to get their stories out.",
            "It's funny because I think a lot of it is simply... We've never considered ourselves satirists, but because we're on comedy central and because we're South Park on comedy central, we can do any topic we want.",
            "There was so long from when we did the pilot and then when the show was eventually picked up by comedy central - and, in fact, we had to shoot the pilot twice.",
            "I'm also doing a special for comedy central called Autobiography. It's going to be a spoof of Biography.",
            "The one that was most fun was That's My Bush; the part that I did for comedy central. That was a hoot. That was more fun that one should be allowed to have.");



    @Override
    public View onCreateInputView() {
        mKeyboardView = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        mKeyboard = new Keyboard(this, R.xml.qwerty);
        Key key = mKeyboard.getModifierKeys().get(0);
        mIcShift = key.icon;
        if (VERSION.SDK_INT >= 21) {
            mIcShiftLocked = getDrawable(R.drawable.ic_shift_locked);
        } else {
            mIcShiftLocked = getResources().getDrawable(R.drawable.ic_shift_locked);
        }

        mKeyboardView.setKeyboard(mKeyboard);
        mKeyboardView.setOnKeyboardActionListener(this);
        return mKeyboardView;
    }

    @Override
    public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        updateCandidates(true);
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        updateCandidates(true);
        setCandidatesViewShown(false);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (newSelEnd > oldSelEnd || newSelEnd == 0) {
            updateCandidates(newSelEnd == 0);
        }
    }

    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        playSFX(primaryCode);

        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;

            case Keyboard.KEYCODE_SHIFT:
                mCaps = !mCaps;
                mKeyboard.setShifted(mCaps);
                Key key = mKeyboard.getModifierKeys().get(0);
                if (mCaps) {
                    key.icon = mIcShiftLocked;
                } else {
                    key.icon = mIcShift;
                }

                mKeyboardView.invalidateAllKeys();
                break;

            case KEYCODE_OK:
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;

            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code) && mCaps){
                    code = Character.toUpperCase(code);
                }

                ic.commitText(String.valueOf(code),1);
        }
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    private void playSFX(int keyCode){
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        switch(keyCode){
            case KEYCODE_SPACE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;

            case Keyboard.KEYCODE_DONE:
            case KEYCODE_OK:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;

            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;

            default:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

    private void updateCandidates(boolean clear) {
        setCandidatesViewShown(!clear);
        if (mCandidateView != null) {
            if (!clear) {
                Random random = new Random();
                int begin = random.nextInt(SUGGESTIONS.size());
                int end = begin + random.nextInt(SUGGESTIONS.size() - begin) + 1;
                mCandidateView.setSuggestions(SUGGESTIONS.subList(begin, end));
            } else {
                mCandidateView.setSuggestions(null);
            }
        }
    }

    public void pickSuggestionManually(String suggestion) {
        getCurrentInputConnection().commitText(" " + suggestion, suggestion.length() + 1);
    }
}
