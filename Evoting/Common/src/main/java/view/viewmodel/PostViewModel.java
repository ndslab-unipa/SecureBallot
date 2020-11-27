package view.viewmodel;

import model.DummyPost;
import utils.Protocol;

public class PostViewModel {
    private DummyPost post;

    public PostViewModel(DummyPost post) {
        this.post = post;
    }

    public int getID() { return post.getId(); }
    public String getState() { return post.getState().toString().replace("_", " "); }
    public String getBadge() { return post.getBadge().equals(Protocol.unassignedPost) ? "---" : post.getBadge(); }

    public String getVoterID() { return post.getVoter() == null ? "---" : post.getVoter().getID(); }
    public String getVoterFirstName() { return post.getVoter() == null ? "---" : post.getVoter().getFirstName(); }
    public String getVoterLastName() { return post.getVoter() == null ? "---" : post.getVoter().getLastName(); }
    
    public boolean isUnreachable() { return post.isUnreachable(); }
}
