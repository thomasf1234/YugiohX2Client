/**
 * Created by tfisher on 31/05/2017.
 */
public class HTTPException extends RuntimeException {
    protected int responseCode;

    public HTTPException(int responseCode) {
        super(String.format("ResponseCode %d", responseCode));
        this.responseCode = responseCode;
    }

    public HTTPException(int responseCode, String message) {
        super(String.format("ResponseCode %d : %s", responseCode, message));
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
