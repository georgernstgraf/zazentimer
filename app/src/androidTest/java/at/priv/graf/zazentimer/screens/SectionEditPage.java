package at.priv.graf.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.action.ViewActions;

import at.priv.graf.zazentimer.R;

public class SectionEditPage extends BasePage {

    public SectionEditPage verifySectionEditScreen() {
        checkElementIsDisplayed(R.id.play_gong);
        checkElementIsDisplayed(R.id.sectionGongVolume);
        return this;
    }

    public SectionEditPage setSectionName(String name) {
        onView(withId(R.id.section_name)).perform(replaceText(name));
        return this;
    }

    public SectionEditPage tapDurationPicker() {
        clickOnView(R.id.duration);
        return this;
    }

    public SectionEditPage setBellCount(int count) {
        int id;
        switch (count) {
            case 1: id = R.id.bellcount1; break;
            case 2: id = R.id.bellcount2; break;
            case 3: id = R.id.bellcount3; break;
            case 4: id = R.id.bellcount4; break;
            case 5: id = R.id.bellcount5; break;
            default: id = R.id.bellcount1; break;
        }
        clickOnView(id);
        return this;
    }

    public SectionEditPage setBellGap(int gap) {
        int id;
        switch (gap) {
            case 1:  id = R.id.gap1;  break;
            case 2:  id = R.id.gap2;  break;
            case 3:  id = R.id.gap3;  break;
            case 4:  id = R.id.gap4;  break;
            case 5:  id = R.id.gap5;  break;
            case 6:  id = R.id.gap6;  break;
            case 7:  id = R.id.gap7;  break;
            case 8:  id = R.id.gap8;  break;
            case 9:  id = R.id.gap9;  break;
            case 10: id = R.id.gap10; break;
            case 11: id = R.id.gap11; break;
            case 12: id = R.id.gap12; break;
            case 13: id = R.id.gap13; break;
            case 14: id = R.id.gap14; break;
            case 15: id = R.id.gap15; break;
            default: id = R.id.gap1;  break;
        }
        onView(withId(id)).perform(ViewActions.scrollTo());
        clickOnView(id);
        return this;
    }

    public SectionEditPage setVolume(int level) {
        onView(withId(R.id.sectionGongVolume)).perform(new androidx.test.espresso.ViewAction() {
            @Override
            public org.hamcrest.Matcher<android.view.View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "set SeekBar progress to " + level;
            }

            @Override
            public void perform(androidx.test.espresso.UiController uiController, android.view.View view) {
                if (view instanceof android.widget.SeekBar) {
                    ((android.widget.SeekBar) view).setProgress(level);
                }
            }
        });
        return this;
    }

    public SectionEditPage clickPlayBell() {
        clickOnView(R.id.play_gong);
        return this;
    }

    public SessionEditPage goBack() {
        pressBack();
        return new SessionEditPage();
    }
}
