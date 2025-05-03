public class ForgotHistoryItem {
    private String data;
    private boolean isMostRecent;
    private boolean isLoading;

    public ForgotHistoryItem(String data, boolean isMostRecent, boolean isLoading) {
        this.data = data;
        this.isMostRecent = isMostRecent;
        this.isLoading = isLoading;
    }

    public String getData() {
        return data;
    }

    public boolean isMostRecent() {
        return isMostRecent;
    }

    public boolean isLoading() {
        return isLoading;
    }
}
