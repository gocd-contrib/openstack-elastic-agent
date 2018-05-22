package cd.go.contrib.elasticagents.openstack;

public class AgentMatchResult {

    private boolean jobMatch;
    private boolean profileMatch;

    public AgentMatchResult(boolean jobMatch, boolean profileMatch) {

        this.jobMatch = jobMatch;
        this.profileMatch = profileMatch;
    }

    public boolean isJobMatch() {
        return jobMatch;
    }

    public boolean isProfileMatch() {
        return profileMatch;
    }
}
