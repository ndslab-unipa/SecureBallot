package view.viewmodel;

import model.Person;

public class VoterViewModel {
    private final Person voter;
    private boolean selected;

    public VoterViewModel(Person voter){
        this.voter = voter;
        this.selected = false;
    }

    public Person getVoter() { return voter; }
    public String getID() { return voter.getID(); }
    public String getFirstName() { return voter.getFirstName(); }
    public String getLastName(){ return voter.getLastName(); }
    public String getBirth(){ return voter.getBirth(); }
    public String getBallotCodesString(){ return voter.getBallotCodesString(); }
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public String getVoteInfo(){
        boolean mayVote = voter.mayVote();
        int[] ballotCodes = voter.getBallotCodes();
        boolean noBallots = ballotCodes == null || ballotCodes.length == 0;
        
        if (mayVote && !noBallots)
        	return "ok";
        
        return !mayVote ? "cant-vote" : "no-ballots";
    }
}
