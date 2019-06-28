package bexchange.eth.api;

/***
 *
 * @author Roberto Santacroce Martins
 *
 */
public class ApiError {

    private long timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String when;


    /***
     * Initializes a new instance of the ApiError class.
     *
     */
    public ApiError() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when;
    }
}
