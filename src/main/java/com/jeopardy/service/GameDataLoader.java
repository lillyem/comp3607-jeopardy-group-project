package com.jeopardy.service;

import com.jeopardy.model.GameData;
import java.io.IOException;
import java.nio.file.Path;

public interface GameDataLoader {
    GameData load(Path path) throws IOException;
}
