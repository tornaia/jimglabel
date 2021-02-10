package com.github.tornaia.jimglabel.common.setting;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserSettingsProvider extends FileBackedAbstractSettingsProvider<UserSettings> {

    @Autowired
    public UserSettingsProvider(SessionSettingsProvider sessionSettingsProvider, SerializerUtils serializerUtils) {
        super("user", UserSettings.class, sessionSettingsProvider, serializerUtils);
    }
}
