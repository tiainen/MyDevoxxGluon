package com.devoxx.model;

import java.util.List;

public class ProposalTypes {

    private String content;
    private List<ProposalType> proposalTypes;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ProposalType> getProposalTypes() {
        return proposalTypes;
    }

    public void setProposalTypes(List<ProposalType> proposalTypes) {
        this.proposalTypes = proposalTypes;
    }
}
