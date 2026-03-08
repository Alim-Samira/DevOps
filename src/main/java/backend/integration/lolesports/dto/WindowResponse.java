package backend.integration.lolesports.dto;

public record WindowResponse(
    String esportsGameId,
    GameMetadata gameMetadata,
    List<Frame> frames
) {}

public record Frame(
    long timestamp,
    TeamFrame blueTeam,
    TeamFrame redTeam,
    List<GameEvent> events
) {}

public record TeamFrame(
    int totalGold,
    int totalKills,
    List<ParticipantFrame> participants
) {}

public record ParticipantFrame(
    String championName,
    int kills, int deaths, int assists,
    int gold, int level,
) {}

public record GameEvent(
    String type,           // "KILL", "DRAGON_KILL", "TOWER_DESTROY", "GAME_END"...
    String killer, 
    String victim,
    String dragonType,     // "ocean", "infernal"...
    String team            // "blue", "red"
) {}

public record GameMetadata(String ... ) {} 