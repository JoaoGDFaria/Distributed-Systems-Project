public class ResultInfo {
    
    private Boolean bool;
    private String error;

    public Boolean getBool() {
        return bool;
    }

    public String getError() {
        return error;
    }

    public ResultInfo(){
        this.bool = false;
        this.error = null;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setTrue() {
        this.bool = true;
    }
}
