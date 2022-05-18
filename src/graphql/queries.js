/* eslint-disable */
// this is an auto generated file. This will be overwritten

export const getGlacierUsers = `query GetGlacierUsers($organization: String!, $username: String!) {
  getGlacierUsers(organization: $organization, username: $username) {
    email
    extension_voiceserver
    first_name
    glacierpwd
    last_name
    messenger_id
    organization
    securityInfo
    selected_twilionumber
    user_name
    username
  }
}
`;
