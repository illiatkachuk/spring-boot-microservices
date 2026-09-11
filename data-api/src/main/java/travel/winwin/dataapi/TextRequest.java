package travel.winwin.dataapi;

/** Request body: { "text": "..." }. A record gives us constructor, getters and equals for free. */
public record TextRequest(String text) {
}
